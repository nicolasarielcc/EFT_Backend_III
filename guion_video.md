# Guión de presentación en video — Banco XYZ (EFT)

> **Formato:** MP4 · **Duración:** 5 a 7 minutos · **Con webcam** + evidencia en pantalla.
> **Instructivo:** Kaltura (https://ava.duoc.cl/bbcswebdav/xid-3577107_1)
> Los puntos del guión corresponden exactamente a los 4 puntos solicitados en las Instrucciones Específicas.

---

## Apertura (≈ 0:15)

**Cámara encendida, presentarse.**

> "Hola, mi nombre es **Nicolás Cavieres**, estudiante de Analista Programador, curso Backend III con el profesor Alonso Castillo. Hoy les presento la Evaluación Final Transversal: *Desarrollo Backend Avanzado: Spring Cloud y Batch*, aplicada al Banco XYZ."

`[PANTALLA → diapositiva de portada]`

---

## Punto 1 — Resumen ejecutivo (≈ 1:15)

**Objetivos y alcance**
> "El Banco XYZ es una institución con más de 30 años, cuyo sistema operaba sobre COBOL y scripts Shell en un mainframe. Debido a su baja escalabilidad, sus altos costos y la dificultad para integrar nuevas tecnologías, se decidió **modernizar la plataforma hacia una arquitectura de microservicios en la nube**."

**Características principales**
> "La solución tiene 3 grandes frentes:
> 1. **Migración de los procesos batch** (transacciones diarias, intereses y estados de cuenta) con Spring Batch.
> 2. **Patrón BFF** con un backend independiente para cada canal: Web, Móvil y Cajeros.
> 3. **Microservicios resilientes y seguros** con Spring Cloud: Config Server, Eureka, API Gateway, OAuth 2.0, Resilience4j y mensajería con Kafka.
> Todo contenerizado con Docker Compose."

`[PANTALLA → diapositiva de arquitectura con mermaid]`

---

## Punto 2 — Resultados obtenidos y comparación con el legacy (≈ 2:00)

**Demostración en vivo (evidencia real)**
> "Veamos los resultados más relevantes. Levanto el stack completo y lo muestro en consola."

`[PANTALLA → live demo]`
- `docker compose ps` — stack sano (13 servicios).
- `docker logs banco-xyz-batch` — los **3 jobs finalizan en COMPLETED**; los registros inválidos se omiten con la política de skip y las anomalías se registran.
- Autenticación de los 3 canales y consulta a un BFF (ej. saldo ATM / detalle Web).

**Optimización por canal**
> "Cada canal recibe una respuesta adaptada: la consulta de cuentas pesa alrededor de **4,5 KB en Web**, **3,15 KB en Móvil** —con solo los últimos 5 movimientos— y **431 bytes en el Cajero**, que solo entrega el saldo. Esto demuestra la optimización real por canal."

**Comparación con el sistema legacy**
> "En el legacy, los procesos descartaban registros con fechas que no estuvieran en formato ISO: de aproximadamente 1.000 movimientos, se perdían cientos. El nuevo proceso **normaliza los 4 formatos de fecha** del dataset, aplica reintentos ante fallas transitorias de base de datos y omisiones controladas. Además, el monolito se dividió en microservicios con tolerancia a fallos, seguridad distribuida y desacoplamiento por eventos: **estabilidad, escalabilidad y mantenibilidad superiores al legacy**."

`[PANTALLA → diapositiva de comparación legacy vs nuevo + resultado del batch]`

---

## Punto 3 — Desafíos enfrentados y soluciones (≈ 2:00)

> "Los principales obstáculos durante el desarrollo fueron:

> **1. Datos legacy sucios.** Montos negativos o nulos, duplicados, fechas en 4 formatos y tipos inválidos. Lo resolví con validación y normalización en el `ItemProcessor`, políticas de `retry`/`skip`, y deduplicación por clave única en la base de datos. Los registros que no se pueden procesar quedan en una tabla de anomalías, nunca se pierden en silencio.

> **2. Consistencia en un entorno distribuido.** Para mantener el saldo actualizado entre microservicios, apliqué una arquitectura orientada a eventos: `ms-pagos` publica la 'transacción completada' en Kafka y `ms-cuentas` la consume para actualizar el saldo (consistencia eventual).

> **3. Fallos en servicios externos.** Implementé **Resilience4j** con Circuit Breaker, Retry y Fallback en los BFF: ante la caída de un microservicio, el canal responde con un fallback controlado (por ejemplo, un cajero nunca dispensa dinero sin confirmar saldo).

> **4. Seguridad distribuida.** Implementé **OAuth 2.0** con un Authorization Server que emite tokens JWT por canal, y cada microservicio actúa como resource server validando el token. Verifiqué los rechazos **401** sin token y **403** sin permiso.

> **5. Despliegue en la nube.** Por las limitaciones del laboratorio AWS Academy, el despliegue real en la nube quedó **documentado** en `despliegue.md` y fuera del alcance evaluado; el sistema fue validado 100 % en local con Docker, manteniendo la portabilidad de las imágenes."

`[PANTALLA → diapositiva desafíos/soluciones]`

---

## Punto 4 — Propuestas de mejora y próximos pasos (≈ 1:00)

> "Para avanzar, propongo:
> 1. **Observabilidad:** OpenTelemetry y trazabilidad distribuida entre los BFF y microservicios.
> 2. **CI/CD:** pipeline automatizado en GitHub Actions desde el build hasta el despliegue.
> 3. **Seguridad:** rotación de claves JWT, mTLS entre servicios y secretos en AWS Secrets Manager.
> 4. **Mensajería en la nube:** desplegar Kafka en EC2 o MSK y conectar los microservicios al broker remoto.
> 5. **Escalabilidad:** particionado de los jobs batch y escalado horizontal real (`--scale`, Auto Scaling).
> 6. **Transacciones distribuidas:** aplicar el patrón Saga con compensación."

`[PANTALLA → diapositiva próximo pasos]`

---

## Cierre (≈ 0:30)

> "Como resumen: el Banco XYZ ya cuenta con una base de **microservicios segura, resiliente y desacoplada**, con los procesos legacy migrados sin pérdida de datos y lista para migrar a la nube. Agradezco su atención, quedo atento a sus comentarios."

`[PANTALLA → diapositiva de cierre]`

---

## Evidencias / pantallazos a capturar durante la demo

| # | Evidencia |
|---|---|
| 1 | `docker compose ps` (stack completo) |
| 2 | Logs del batch: 3 jobs `COMPLETED` + skips |
| 3 | Eureka (http://localhost:8761) con microservicios registrados |
| 4 | Login de los 3 canales (tokens JWT) y consultas a BFFs |
| 5 | 401 (sin token) y 403 (sin permiso) |
| 6 | Fallback Resilience4j al detener `ms-cuentas` |
| 7 | Kafka UI / `kafka-console-consumer` (retiros/transacciones) |
| 8 | `GET /api/notificaciones` (eventos consumidos) |

## Checklist de la entrega

- [ ] Código fuente en repositorio GitHub (referenciado en `readme.md`).
- [ ] Informe técnico en PDF (pegar el contenido en la plantilla `PBY2203_EFT_S9_plantilla_PDF`).
- [ ] `instrucciones.md` (ejecutar y probar cada componente) + colección Postman.
- [ ] `despliegue.md` (pasos de despliegue en la nube; documento, fuera de evaluación por AWS Academy).
- [ ] Video MP4 5–7 min según este guión.
- [ ] Todo en una misma carpeta + video.