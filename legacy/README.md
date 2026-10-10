# Procesos Batch Legacy (copia original) vs Spring Batch (reescritura)

> Requerimiento 1.a de la EFT: "Tendremos una copia en legacy y otra reescrita para los procesos en Spring Batch".

## 1. Qué contiene esta carpeta

- `LegacyProcesos.java` — implementación **legacy** de los 3 procesos batch (simula el mainframe COBOL + Shell con Java plano, sin framework). Solo entiende fechas `YYYY-MM-DD`, no normaliza, no reintenta y no registra anomalías.

La **versión reescrita** vive en el módulo [`../banco-batch`](../banco-batch) (Spring Batch) y produce resultados **equivalentes** pero corrigiendo las limitaciones del legacy.

## 2. Cómo ejecutar los procesos legacy

```bash
# compilar
javac -d /tmp/legacy legacy/LegacyProcesos.java
# ejecutar contra los CSV de fin_legacy_data
java -cp /tmp/legacy legacy.LegacyProcesos \
  banco-batch/src/main/resources/data \
  /tmp/legacy_output
```

## 3. Resultado de la ejecución legacy (sobre fin_legacy_data)

| Proceso | Procesados | Descartados | Causa de descarte |
|---|---|---|---|
| Reporte de Transacciones Diarias | 221 | **799** | Fechas no ISO, montos vacíos/negativos, tipos inválidos |
| Cálculo de Intereses | 811 | (solo inválidos) | Saldos no numéricos |
| Estados de Cuenta Anuales | 233 | **784** | Fechas no ISO (`YYYY/MM/DD`, `DD-MM-YYYY`, `DD/MM/YYYY`) |

## 4. Por qué se reescribe: el legacy pierde datos "sucios" reales

El dataset oficial **mezcla 4 formatos de fecha**:

```
2024-01-01  (ISO)
2024/03/22  (YYYY/MM/DD)
25-11-2024  (DD-MM-YYYY)
02/01/2024  (DD/MM/YYYY)
```

El legacy solo procesa ISO → **descarta ~1600 registros válidos** que sí deben formar parte de los estados financieros.

## 5. Equivalencia y correcciones (Spring Batch)

| Requerimiento | Legacy | Spring Batch (`banco-batch`) |
|---|---|---|
| 1.a Copia original + reescrita | Java plano sin framework | Jobs `reporteTransaccionesJob`, `calculoInteresesJob`, `estadosAnualesJob` |
| 1.b Excepciones/reintentos | No | `RetryPolicy` para fallas transitorias de BD + `SkipPolicy` para inválidos |
| 1.c Grandes volúmenes | Secuencial | Chunking + particionado/paralelismo configurable (`batch.chunk-size`, `grid-size`) |
| 1.d Finalización / reejecución | No | `JobRepository` + reinicio (`restart`) + clean previo (idempotente) + `BATCH_RUN_ON_STARTUP` |
| 1.e Integridad/consistencia | Pérdida de montos negativos/cero | Validación en `ItemProcessor`, deduplicación por clave única (`INSERT IGNORE`), anomalías registradas en tabla `anomalia` |
| 1.f Equivalencia de resultados | Descarta ~1600 | **Normaliza los 4 formatos de fecha** y procesa el 100% (válidos a tablas, inválidos a `anomalia` + resumen) |

> **Equivalencia funcional:** ambos producen las mismas 3 salidas (transacciones, cuentas/intereses, estados); la diferencia es que Spring Batch procesa (o registra como anomalía) lo que el legacy descartaba silenciosamente, con tolerancia a fallos y trazabilidad.

## 6. Evidencia de la normalización de fechas

El parser `ParseoUtil.parseFecha` de `banco-batch` cubre los 4 formatos del dataset:

```java
private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // ISO
    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
```

Verificación sobre el dataset oficial: **0 fechas inválidas** en `estados_financieros_anuales.csv` y en `movimientos_financieros_diarios.csv` solo los campos vacíos (registrados como anomalías).