# Guion para el video de presentación (EFT)

> Formato: MP4, 5–7 minutos, con webcam + evidencia en pantalla (instructivo Kaltura).

## Estructura sugerida (los 4 puntos de la pauta)

### 1. Resumen ejecutivo (~1 min)
- Presentarse (nombre, carrera, curso).
- "El Banco XYZ migra su sistema legacy COBOL/mainframe a microservicios en la nube."
- Mencionar los 3 frentes: Spring Batch, BFF y microservicios Spring Cloud.
- Mostrar el diagrama de arquitectura.

### 2. Resultados y comparación con el legacy (~2 min)
- Demo en vivo del stack (docker compose ps + Eureka + Kafka UI).
- Ejecutar el batch (`GET /api/batch/procesar`) y mostrar `COMPLETED`.
- Mostrar tabla comparativa legacy vs nuevo sistema (del informe).

### 3. Desafíos y soluciones (~2 min)
- Datos legacy sucios → retry/skip + deduplicación.
- Consistencia distribuida → eventos Kafka.
- Tolerancia a fallos → Resilience4j (mostrar fallback al detener ms-cuentas).
- Seguridad → OAuth 2.0 (mostrar 401/403).

### 4. Propuestas de mejora y próximos pasos (~1 min)
- Observabilidad (tracing), CI/CD, patrón Saga, BD por servicio.

## Evidencia a grabar en pantalla
1. `docker compose ps` — stack completo.
2. Eureka (http://localhost:8761) — microservicios registrados.
3. `docker logs banco-xyz-batch` — batch COMPLETED.
4. Login de los 3 canales + peticiones a los BFFs.
5. 401 (sin token) y 403 (sin permiso).
6. Fallback de Resilience4j al detener `ms-cuentas`.
7. Kafka UI / `kafka-console-consumer` — eventos retiros/transacciones.
8. `GET /api/notificaciones` — eventos consumidos.
