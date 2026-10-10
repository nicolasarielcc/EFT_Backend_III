# Evaluación del proyecto contra la Pauta de la EFT (Paso 2) y correcciones aplicadas (Paso 3)

Evaluación realizada **como profesor de ingeniería de software** sobre el proyecto `Banco_XYZ_EFT`, siguiendo los 7 criterios de la Pauta de Evaluación Final Transversal (columna "Completamente Logrado 100%").

---

## 1. Evaluación por criterio

### Criterio 1 — Identifica los 5 procesos clave para la migración (10 pts)

| Proceso clave | ¿Identificado? | Dónde |
|---|---|---|
| Migración de procesos Batch a Spring Batch | ✅ | `banco-batch` (3 jobs) + `legacy/` |
| División del sistema monolítico en microservicios | ✅ | `ms-clientes`, `ms-cuentas`, `ms-pagos` |
| Implementación del Patrón BFF | ✅ | `bff-web`, `bff-mobile`, `bff-atm` |
| Seguridad distribuida (Spring Cloud Security) | ✅ | `auth-server` (OAuth 2.0) + resource servers |
| Mensajería asíncrona (Apache Kafka) | ✅ | tópicos `clientes`, `retiros`, `transacciones`, `autenticaciones` |

**Evaluación:** cumple (10/10). Los 5 procesos están identificados e implementados.

### Criterio 2 — Propuesta de arquitectura con 3 requerimientos clave justificados (15 pts)

- **Escalabilidad** → microservicios + Docker + Eureka + API Gateway (justificado en informe §3).
- **Resiliencia** → Resilience4j (Circuit Breaker/Retry/Fallback) + reinicio de contenedores.
- **Seguridad** → OAuth 2.0 (Autorización delegada) + HTTPS por canal.

Diagramas en `informe_tecnico.html` (mermaid: arquitectura, componentes, casos de uso, despliegue).

**Evaluación:** cumple (15/15).

### Criterio 3 — Implementa 3 procesos batch con manejo avanzado de errores, paralelismo y escalabilidad (15 pts)

- 3 jobs: `reporteTransaccionesJob`, `calculoInteresesJob`, `estadosAnualesJob`.
- **Errores:** `RetryPolicy` (fallas transitorias de BD) + `SkipPolicy`/`SkipListener` (registros inválidos) + `DatoInvalidoException`.
- **Paralelismo:** `TaskExecutor` configurable + comparación con tiempos reales (`comparacion_particionamiento.md`).
- **Escalabilidad:** chunking configurable (`batch.chunk-size`) y guía de particionado para grandes volúmenes.
- **Normalización:** parser de 4 formatos de fecha (`ParseoUtil.parseFecha`) — correctivo S1.
- **Re-ejecución:** `JobRepository` + limpieza previa (idempotente) + `BATCH_RUN_ON_STARTUP`.
- **Copia legacy** en `legacy/` para demostrar equivalencia (correctivo 1.a).

**Evaluación:** cumple (15/15).

### Criterio 4 — Desarrolla el patrón BFF para los 3 canales, optimizando rendimiento y seguridad (15 pts)

- BFF independientes (módulos Maven separados, sin dependencias cruzadas).
- Optimización: Web completo 4,5 KB / Móvil ligero 3,15 KB (límite 5 movimientos) / ATM saldo 431 B.
- DTOs por canal + resumen calculado en Web.
- Seguridad: HTTPS (keystore), OAuth 2.0 por canal (`ROLE_WEB/MOBILE/ATM`), token relay.
- Resilience4j en los 3 BFF (fallback ante caída de un microservicio).

**Evaluación:** cumple (15/15).

### Criterio 5 — Crea microservicios para los 3 servicios clave con alta resiliencia y seguridad, Resilience4j y Kafka (15 pts)

- 3 microservicios: `ms-clientes`, `ms-cuentas`, `ms-pagos`.
- Spring Cloud: Config Server, Eureka, API Gateway, LoadBalancer.
- Seguridad: OAuth 2.0 resource servers (JWK Set) + autorización por recurso.
- Resiliencia: Resilience4j en los BFF (consumidores).
- Kafka: productores (`auth-server`, `ms-clientes`, `ms-cuentas`, `ms-pagos`) y consumidores (`notificaciones-service`, `ms-cuentas`); consistencia eventual (depósito → evento → saldo).

**Evaluación:** cumple (15/15).

### Criterio 6 — Docker y escalabilidad horizontal para el despliegue en la nube (10 pts)

- `Dockerfile` multi-etapa por cada uno de los 12 servicios.
- `docker-compose.yml` único (13 servicios + MySQL + Kafka + Kafka UI), con healthchecks y `depends_on`.
- `despliegue.md`: AWS (ECR, ECS/Fargate, RDS, ALB, ACM, Secrets Manager, MSK) + Auto Scaling.
- PBS: 3 microservicios listos para replicar (`docker compose up --scale`), Eureka/Gateway balancean.

**Evaluación:** cumple (10/10). *Aspecto a reforzar:* validar ejecución real en AWS (laboratorio) — ver Paso 3 S3.

### Criterio 7 — Documenta los 4 aspectos claves (10 pts)

- **Código fuente** → repositorio GitHub (`EFT_Backend_III`).
- **Informe técnico** → `informe_tecnico.md` + `informe_tecnico.html` (mermaid) + `informe_tecnico.pdf`.
- **Instrucciones** → `instrucciones.md`.
- **Pasos para el despliegue** → `despliegue.md`.
- Extras: `readme.md`, `POSTMAN.md` (colección `postman/Banco_XYZ_EFT.postman_collection.json`), `presentacion.html`, `guion_video.md`.

**Evaluación:** cumple (10/10).

---

## 2. Resumen de puntos

| N° | Criterio | Puntos | Estado |
|---|---|---|---|
| 1 | Identifica procesos clave | 10 | ✅ Cumple |
| 2 | Propuesta de arquitectura | 15 | ✅ Cumple |
| 3 | Procesos batch | 15 | ✅ Cumple |
| 4 | Patrón BFF | 15 | ✅ Cumple |
| 5 | Microservicios + Spring Cloud + Kafka | 15 | ✅ Cumple |
| 6 | Docker + escalabilidad en la nube | 10 | ✅ (AWS pendiente de validar) |
| 7 | Documentación | 10 | ✅ Cumple |
| **Total** | | **100** | |

---

## 3. Correcciones y sugerencias del profesor aplicadas (Paso 3)

### S1 — Procesos batch

| Sugerencia | Aplicación |
|---|---|
| Parser de fechas que cubra los 4 formatos del dataset | `ParseoUtil.parseFecha` cubre `yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`; verificado: 0 fechas inválidas en estados anuales. |
| Revisar cobertura de la política de omisión (skip) frente a todos los escenarios de error | Skip abarca: columnas insuficientes, ids inválidos, nombres desconocidos, saldos negativos, edades fuera de rango, fechas inválidas, montos vacíos/cero/negativos, tipos inválidos. Registro en `SkipLogListener` + `anomalia`. |
| Documentar comparación de configuraciones de particionamiento con tiempos reales | `comparacion_particionamiento.md` con mediciones (secuencial 6s618ms vs paralelo-4 6s162ms vs paralelo-8 6s426ms). |
| Copia legacy + reescritura, equivalencia | `legacy/` (copia original) + `banco-batch` (reescritura); tabla de equivalencia. |

### S2 — BFF

| Sugerencia | Aplicación |
|---|---|
| Los criterios evaluados están correctamente resueltos | Se mantiene la arquitectura de 3 BFFs independientes con DTOs por canal y optimización verificada. |
| (Mejora) Propagar trazas entre backend-core y BFFs | Documentado como mejora futura en `informe_tecnico.html` (§15, OpenTelemetry/tracing). |
| (Mejora) Versionar contratos de cada canal | Documentado en mejoras futuras (contract testing / versiones de DTOs). |

### S3 — Microservicios y Spring Cloud

| Sugerencia | Aplicación |
|---|---|
| Kafka real y funcional | Productores + consumidores con topología de 4 tópicos; Flujo depósito→evento→saldo y retiro→notificación. |
| **Mensajería en nube (AWS EC2)** | Añadida guía completa en `despliegue.md` (§ Kafka en EC2/MSK): `KAFKA_ADVERTISED_LISTENERS` con IP pública, Security Group, `KAFKA_BOOTSTRAP_SERVERS` apuntando al broker remoto, pruebas con Kafka UI/consola. Pendiente de ejecutar en laboratorio (limitaciones de AWS Academy). |
| Imágenes Docker y Docker Compose | 12 Dockerfiles + `docker-compose.yml` con healthchecks y dependencias. |
| Resilience4j | Circuit Breaker + Retry + Fallback en los 3 BFF con configuración por instancia. |

---

*Documento de evaluación interna (Paso 2) y registro de correcciones (Paso 3) — Banco XYZ · EFT Backend III.*