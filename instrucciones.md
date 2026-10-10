# Instrucciones para ejecutar y probar cada componente — Banco XYZ (EFT)

Este documento describe paso a paso cómo ejecutar y probar cada componente del sistema, de forma que cualquier persona pueda verificar su funcionamiento.

---

## 0. Requisitos previos

- Docker y Docker Compose instalados.
- Opcional: Postman (desactivar verificación SSL para los BFF, que usan HTTPS autofirmado: *Settings → General → SSL certificate verification → OFF*).

---

## 1. Despliegue del sistema

```bash
cd Banco_XYZ_EFT
docker compose up -d --build
```

Esperar a que todos los contenedores estén *healthy*/arrancados:

```bash
docker compose ps
```

> El `banco-batch` se ejecuta automáticamente al arrancar (`BATCH_RUN_ON_STARTUP=true`) y carga los datos de `fin_legacy_data` en MySQL.

---

## 2. Verificar la infraestructura Spring Cloud

### 2.1 Config Server
```bash
curl http://localhost:8888/actuator/health
curl http://localhost:8888/application/default   # configuración compartida
```

### 2.2 Eureka (Service Discovery)
Abrir en el navegador: **http://localhost:8761**

**Qué verificar:** los microservicios `MS-CLIENTES`, `MS-CUENTAS`, `MS-PAGOS` y `API-GATEWAY` aparecen registrados en la lista de instancias.

### 2.3 API Gateway
```bash
# Debe enrutar hacia ms-cuentas (sin token devuelve 401, lo que confirma el enrutado)
curl -i http://localhost:8087/api/cuentas
```

### 2.4 Kafka UI
Abrir en el navegador: **http://localhost:8090** → ver los tópicos `autenticaciones`, `clientes`, `retiros`, `transacciones`.

---

## 3. Procesos Batch (Spring Batch)

Los tres jobs pueden dispararse individualmente o todos juntos:

```bash
# 1) Reporte de Transacciones Diarias (movimientos + anomalías + resumen)
curl http://localhost:8080/api/batch/reporte-transacciones

# 2) Cálculo de Intereses (cuentas)
curl http://localhost:8080/api/batch/calculo-intereses

# 3) Generación de Estados de Cuenta Anuales
curl http://localhost:8080/api/batch/estados-anuales

# Los tres juntos
curl http://localhost:8080/api/batch/procesar
```

**Qué verificar:** la respuesta JSON incluye `estado: COMPLETED` y los contadores `leidos`, `escritos`, `omitidos`. Los registros inválidos (montos negativos, fechas mal formadas, tipos desconocidos) se omiten (`omitidos > 0`).

**Evidencia (logs del batch):**
```bash
docker logs banco-xyz-batch | tail -50
```

---

## 4. Autenticación por canal (OAuth 2.0)

Cada canal obtiene su token con `POST /api/auth/login` (client credentials). Guardar el token de cada canal:

```bash
# Canal Web (HTTPS autofirmado -> usar -k)
TOKEN_WEB=$(curl -sk -X POST https://localhost:8082/api/auth/login \
  -H 'Content-Type: application/json' -d '{}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")

# Canal Móvil
TOKEN_MOBILE=$(curl -sk -X POST https://localhost:8083/api/auth/login \
  -H 'Content-Type: application/json' -d '{}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")

# Canal Cajero
TOKEN_ATM=$(curl -sk -X POST https://localhost:8084/api/auth/login \
  -H 'Content-Type: application/json' -d '{}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
```

**Qué verificar:** cada `login` devuelve un `accessToken` JWT con el rol del canal (`ROLE_WEB`, `ROLE_MOBILE`, `ROLE_ATM`).

---

## 5. Probar los microservicios (vía API Gateway)

### 5.1 Gestión de Clientes (`ms-clientes`)
```bash
# Crear cliente (requiere ROLE_WEB)
curl -i http://localhost:8087/api/clientes \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d '{"nombre":"Ana Torres","email":"ana@example.com","telefono":"+56 9 1234 5678","direccion":"Santiago"}'

# Listar clientes
curl -i http://localhost:8087/api/clientes -H "Authorization: Bearer $TOKEN_WEB"

# Obtener cliente por id
curl -i http://localhost:8087/api/clientes/1 -H "Authorization: Bearer $TOKEN_WEB"
```

### 5.2 Gestión de Cuentas (`ms-cuentas`)
```bash
# Listar cuentas (cargadas por el batch)
curl -i http://localhost:8087/api/cuentas -H "Authorization: Bearer $TOKEN_WEB"

# Consultar saldo de una cuenta (101 existe tras el batch)
curl -i http://localhost:8087/api/cuentas/101/saldo -H "Authorization: Bearer $TOKEN_ATM"
```

### 5.3 Procesamiento de Pagos (`ms-pagos`)
```bash
# Realizar un depósito (se publica evento "transacciones" en Kafka)
curl -i -X POST http://localhost:8087/api/pagos/depositos \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d '{"cuentaId":101,"tipo":"deposito","monto":500,"descripcion":"Depósito sucursal"}'

# Transacciones de una cuenta
curl -i http://localhost:8087/api/transacciones/cuenta/101 -H "Authorization: Bearer $TOKEN_WEB"
```

---

## 6. Probar los BFF (canales)

### 6.1 Canal Web (datos completos)
```bash
curl -sk https://localhost:8082/api/web/cuentas -H "Authorization: Bearer $TOKEN_WEB"
curl -sk https://localhost:8082/api/web/cuentas/101 -H "Authorization: Bearer $TOKEN_WEB"
```
**Qué verificar:** la respuesta incluye saldo, tipo, movimientos y un resumen (totales por tipo).

### 6.2 Canal Móvil (respuestas ligeras)
```bash
curl -sk https://localhost:8083/api/mobile/cuentas -H "Authorization: Bearer $TOKEN_MOBILE"
curl -sk https://localhost:8083/api/mobile/cuentas/101 -H "Authorization: Bearer $TOKEN_MOBILE"
```
**Qué verificar:** respuesta reducida (solo id, nombre, saldo + últimos movimientos).

### 6.3 Canal Cajero (saldo y retiro)
```bash
# Consulta de saldo
curl -sk https://localhost:8084/api/atm/cuentas/101/saldo -H "Authorization: Bearer $TOKEN_ATM"

# Retiro (operación crítica, solo ROLE_ATM)
curl -sk -X POST https://localhost:8084/api/atm/cuentas/101/retiros \
  -H "Authorization: Bearer $TOKEN_ATM" -H 'Content-Type: application/json' \
  -d '{"monto":100}'
```
**Qué verificar:** el retiro devuelve `estado: APROBADO` y actualiza el saldo. Publica el evento `retiros` en Kafka.

### 6.4 Notificaciones (consumidor Kafka)
```bash
curl http://localhost:8085/api/notificaciones
```
**Qué verificar:** lista los eventos `retiros` consumidos desde Kafka (flujo productor → consumidor).

---

## 7. Verificar seguridad (401 / 403)

```bash
# Sin token -> 401
curl -i http://localhost:8087/api/cuentas

# Retiro con un canal sin permisos (ROLE_WEB) -> 403
curl -i -X POST http://localhost:8087/api/cuentas/101/retiros \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d '{"monto":100}'
```

---

## 8. Tolerancia a fallos (Resilience4j)

1. Detener el microservicio de cuentas para forzar un fallo:
   ```bash
   docker stop banco-xyz-ms-cuentas
   ```
2. Llamar al BFF Web (usa Circuit Breaker + Fallback):
   ```bash
   curl -sk https://localhost:8082/api/web/cuentas -H "Authorization: Bearer $TOKEN_WEB"
   ```
   **Qué verificar:** la respuesta no rompe el BFF; se activa el *fallback* (lista vacía / "No disponible").
3. Volver a levantar el servicio:
   ```bash
   docker start banco-xyz-ms-cuentas
   ```

---

## 9. Evidencia de Kafka (consola / UI)

```bash
# Listar tópicos
docker exec banco-xyz-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describir el tópico de transacciones (particiones)
docker exec banco-xyz-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic transacciones

# Consumir el tópico de retiros (desde el inicio)
docker exec banco-xyz-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic retiros --from-beginning
```

---

## 10. Resumen de pantallazos mínimos (evidencia)

| # | Bloque | Pantallazo |
|---|---|---|
| 1 | Docker | `docker compose ps` (stack completo) |
| 2 | Eureka | Panel con los microservicios registrados |
| 3 | Batch | `docker logs banco-xyz-batch` (COMPLETED) |
| 4 | Auth | Login de los 3 canales (tokens JWT) |
| 5 | Microservicios | clientes/cuentas/pagos vía Gateway |
| 6 | BFF | Web (detalle), Móvil (ligero), ATM (saldo/retiro) |
| 7 | Seguridad | 401 sin token y 403 sin permiso |
| 8 | Resilience4j | Fallback al detener ms-cuentas |
| 9 | Kafka | `--list`, `--describe` y `kafka-console-consumer` |
| 10 | Notificaciones | `GET /api/notificaciones` con eventos consumidos |
