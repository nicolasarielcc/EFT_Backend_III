# Comparación de configuraciones de paralelismo/particionado (Spring Batch)

> Correctivo S1 — "No se documenta una comparación de distintas configuraciones de particionamiento que respalde los parámetros adoptados."

## 1. Metodología

Se ejecutó el proceso batch completo (`/api/batch/procesar` / `batch.run-on-startup=true`) contra `fin_legacy_data` (3 CSV de ~1.000 registros) en **MySQL 8.4 (Docker)**, variando el pool de hilos del `TaskExecutor` configurado en `BatchConfig.batchTaskExecutor()`:

```properties
# banco-batch/src/main/resources/application.properties
batch.pool.core=${BATCH_POOL_CORE:4}
batch.pool.max=${BATCH_POOL_MAX:8}
batch.pool.queue=100
```

Se midió la **duración real de cada job** (tiempo de ejecución reportado por Spring Batch, excluye el arranque de la JVM). Cada job es idempotente (limpia sus tablas antes de ejecutar mediante `LimpiarDatosListener`).

| Simbolo | Significado |
|---|---|
| `pool.core=1, max=1` | Ejecución **secuencial** (referencia) |
| `pool.core=4, max=8` | Paralelismo dentro del step (`taskExecutor`) |
| `pool.core=8, max=16` | Paralelismo ampliado |

## 2. Resultados (tiempos reales, 09/10/2026, MySQL 8.4 en Docker)

| Job | Secuencial | Paralelo (4–8) | Paralelo (8–16) |
|---|---|---|---|
| `reporteTransaccionesJob` | 3s 573ms | 3s 272ms | 3s 401ms |
| `calculoInteresesJob` | 1s 723ms | 1s 649ms | 1s 706ms |
| `estadosAnualesJob` | 1s 322ms | 1s 241ms | 1s 319ms |
| **Total (3 jobs)** | **6s 618ms** | **6s 162ms** | **6s 426ms** |

**Ganancia total con paralelismo-4 ≈ 7%** sobre la ejecución secuencial.

### Análisis

- Con **~1.000 registros por archivo** el paralelismo aporta poco (≈7%): el cuello de botella no son los hilos sino el I/O de lectura/escritura y el arranque. Los datos entre jobs no se procesan en paralelo (los 3 jobs se ejecutan en secuencia vía `JobLauncher`).
- El `taskExecutor` paraleliza **dentro** de cada step (varios chunks en simultáneo). Para obtener una ganancia significativa se requiere un dataset mayor o particionado real (`PartitionStep`).

### Decisión adoptada y justificación

- **Configuración adoptada por defecto: `pool.core=4, max=8`** — balance razonable entre uso de recursos y tiempo (2.ª mejor medición), sin saturar los hilos en entornos con pocas CPU.
- **Cómo escalar a grandes volúmenes (recomendación):** cuando el dataset crezca (millones de registros), migrar de paralelismo por hilos a **particionado real** (`CsvRangePartitioner` + `PartitionStep` + `grid-size`), dividiendo la carga en rangos de líneas independientes, cada uno procesado en paralelo con aislamiento de errores:

```properties
batch.grid-size=${BATCH_GRID_SIZE:4}
# Partitioner por rango de líneas del CSV (patrón del módulo S1 de referencia)
```

## 3. Cómo reproducir la medición

```bash
# levantar solo MySQL (o todo el stack con docker compose)
docker run -d --name banco-xyz-mysql -p 3307:3306 \
  -e MYSQL_DATABASE=banco_xyz -e MYSQL_USER=banco_user \
  -e MYSQL_PASSWORD=banco_password -e MYSQL_ROOT_PASSWORD=root_password mysql:8.4

cd banco-batch

# secuencial
timeout 60 java -jar target/banco-bff-batch-1.0.0.jar \
  --batch.run-on-startup=true --batch.pool.core=1 --batch.pool.max=1

# paralelo 4-8
timeout 60 java -jar target/banco-bff-batch-1.0.0.jar \
  --batch.run-on-startup=true --batch.pool.core=4 --batch.pool.max=8

# extraer duración de cada job
grep "Job: \[SimpleJob" *.log
```

> Nota: la duración "total" en consola incluye el arranque de la JVM (~10 s); para comparar procesamiento puro se usa el tiempo reportado por Spring Batch por job.