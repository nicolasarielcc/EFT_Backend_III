package cl.duoc.banco.bff.batch.config;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.banco.bff.batch.exception.DatoInvalidoException;
import cl.duoc.banco.bff.batch.listener.LimpiarDatosListener;
import cl.duoc.banco.bff.batch.listener.SkipLogListener;
import cl.duoc.banco.bff.batch.model.Anomalia;
import cl.duoc.banco.bff.batch.model.Cuenta;
import cl.duoc.banco.bff.batch.model.Movimiento;
import cl.duoc.banco.bff.batch.model.Transaccion;
import cl.duoc.banco.bff.batch.processor.AnomaliaProcessor;
import cl.duoc.banco.bff.batch.processor.CuentaItemProcessor;
import cl.duoc.banco.bff.batch.processor.MovimientoItemProcessor;
import cl.duoc.banco.bff.batch.processor.TransaccionItemProcessor;
import cl.duoc.banco.bff.batch.tasklet.ResumenTransaccionesTasklet;
import lombok.RequiredArgsConstructor;

/**
 * Configuracion de los 3 procesos batch del Banco XYZ (EFT) sobre
 * fin_legacy_data:
 *
 * <ol>
 *   <li>Reporte de Transacciones Diarias (movimientos + deteccion de anomalias
 *       + resumen).</li>
 *   <li>Calculo de Intereses (cuentas de ahorro/prestamo).</li>
 *   <li>Generacion de Estados de Cuenta Anuales.</li>
 * </ol>
 *
 * <p>Cada job aplica politicas de skip (registros invalidos) y retry (fallas
 * transitorias de conexion a la base de datos).</p>
 */
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private static final int SKIP_LIMIT = 1_000_000;
    private static final int RETRY_LIMIT = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;

    private final CuentaItemProcessor cuentaItemProcessor;
    private final TransaccionItemProcessor transaccionItemProcessor;
    private final MovimientoItemProcessor movimientoItemProcessor;
    private final AnomaliaProcessor anomaliaProcessor;
    private final ResumenTransaccionesTasklet resumenTransaccionesTasklet;

    private final SkipLogListener skipLogListener;
    private final LimpiarDatosListener limpiarDatosListener;

    @Value("${batch.data.intereses:data/intereses_trimestrales.csv}")
    private String interesesPath;

    @Value("${batch.data.estados-anuales:data/estados_financieros_anuales.csv}")
    private String estadosAnualesPath;

    @Value("${batch.data.movimientos:data/movimientos_financieros_diarios.csv}")
    private String movimientosPath;

    @Value("${batch.chunk-size:50}")
    private int chunkSize;

    @Value("${batch.pool.core:1}")
    private int poolCore;

    @Value("${batch.pool.max:1}")
    private int poolMax;

    @Value("${batch.pool.queue:100}")
    private int poolQueue;

    // =====================================================================
    // Paralelismo (configurable para comparar configuraciones, correctivo S1)
    // =====================================================================

    /**
     * Pool de hilos para procesamiento paralelo de los steps.
     * {@code poolCore=1} emula ejecucion secuencial (comparativa).
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor pool =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        pool.setCorePoolSize(poolCore);
        pool.setMaxPoolSize(poolMax);
        pool.setQueueCapacity(poolQueue);
        pool.setThreadNamePrefix("batch-worker-");
        pool.initialize();
        return pool;
    }

    // =====================================================================
    // Jobs
    // =====================================================================

    /** Reporte de Transacciones Diarias: carga movimientos validos, detecta anomalias y genera resumen. */
    @Bean
    public Job reporteTransaccionesJob() {
        return new JobBuilder("reporteTransaccionesJob", jobRepository)
                .listener(limpiarDatosListener)
                .start(movimientosStep())
                .next(anomaliasStep())
                .next(resumenStep())
                .build();
    }

    /** Calculo de Intereses: procesa las cuentas (ahorro/prestamo). */
    @Bean
    public Job calculoInteresesJob() {
        return new JobBuilder("calculoInteresesJob", jobRepository)
                .listener(limpiarDatosListener)
                .start(cuentasStep())
                .build();
    }

    /** Generacion de Estados de Cuenta Anuales: procesa el historial de operaciones. */
    @Bean
    public Job estadosAnualesJob() {
        return new JobBuilder("estadosAnualesJob", jobRepository)
                .listener(limpiarDatosListener)
                .start(transaccionesStep())
                .build();
    }

    // =====================================================================
    // Steps
    // =====================================================================

    @Bean
    public Step cuentasStep() {
        return new StepBuilder("cuentasStep", jobRepository)
                .<String, Cuenta>chunk(chunkSize, transactionManager)
                .reader(rawReader(interesesPath))
                .processor(cuentaItemProcessor)
                .writer(cuentaWriter())
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(SKIP_LIMIT)
                .retry(TransientDataAccessException.class)
                .retryLimit(RETRY_LIMIT)
                .listener(skipLogListener)
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step transaccionesStep() {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<String, Transaccion>chunk(chunkSize, transactionManager)
                .reader(rawReader(estadosAnualesPath))
                .processor(transaccionItemProcessor)
                .writer(transaccionWriter())
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(SKIP_LIMIT)
                .retry(TransientDataAccessException.class)
                .retryLimit(RETRY_LIMIT)
                .listener(skipLogListener)
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step movimientosStep() {
        return new StepBuilder("movimientosStep", jobRepository)
                .<String, Movimiento>chunk(chunkSize, transactionManager)
                .reader(rawReader(movimientosPath))
                .processor(movimientoItemProcessor)
                .writer(movimientoWriter())
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(SKIP_LIMIT)
                .retry(TransientDataAccessException.class)
                .retryLimit(RETRY_LIMIT)
                .listener(skipLogListener)
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step anomaliasStep() {
        return new StepBuilder("anomaliasStep", jobRepository)
                .<String, Anomalia>chunk(chunkSize, transactionManager)
                .reader(rawReader(movimientosPath))
                .processor(anomaliaProcessor)
                .writer(anomaliaWriter())
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step resumenStep() {
        return new StepBuilder("resumenStep", jobRepository)
                .tasklet(resumenTransaccionesTasklet, transactionManager)
                .build();
    }

    // =====================================================================
    // Writers
    // =====================================================================

    public ItemWriter<Cuenta> cuentaWriter() {
        return items -> {
            List<Cuenta> list = new ArrayList<>();
            for (Cuenta c : items) {
                list.add(c);
            }
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO cuenta (cuenta_id, nombre, saldo, edad, tipo) VALUES (?, ?, ?, ?, ?)",
                    list, list.size(), (ps, c) -> {
                        ps.setLong(1, c.getCuentaId());
                        ps.setString(2, c.getNombre());
                        ps.setBigDecimal(3, c.getSaldo());
                        ps.setInt(4, c.getEdad());
                        ps.setString(5, c.getTipo());
                    });
        };
    }

    public ItemWriter<Transaccion> transaccionWriter() {
        return items -> {
            List<Transaccion> list = new ArrayList<>();
            for (Transaccion t : items) {
                list.add(t);
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO transaccion (cuenta_id, fecha, tipo, monto, descripcion) VALUES (?, ?, ?, ?, ?)",
                    list, list.size(), (ps, t) -> {
                        ps.setLong(1, t.getCuentaId());
                        ps.setDate(2, Date.valueOf(t.getFecha()));
                        ps.setString(3, t.getTipo());
                        ps.setBigDecimal(4, t.getMonto());
                        ps.setString(5, t.getDescripcion());
                    });
        };
    }

    public ItemWriter<Movimiento> movimientoWriter() {
        return items -> {
            List<Movimiento> list = new ArrayList<>();
            for (Movimiento m : items) {
                list.add(m);
            }
            jdbcTemplate.batchUpdate(
                    "INSERT IGNORE INTO movimiento (id, fecha, monto, tipo) VALUES (?, ?, ?, ?)",
                    list, list.size(), (ps, m) -> {
                        ps.setLong(1, m.getId());
                        ps.setDate(2, Date.valueOf(m.getFecha()));
                        ps.setBigDecimal(3, m.getMonto());
                        ps.setString(4, m.getTipo());
                    });
        };
    }

    public ItemWriter<Anomalia> anomaliaWriter() {
        return items -> {
            for (Anomalia a : items) {
                jdbcTemplate.update(
                        "INSERT INTO anomalia (fecha, monto, tipo, motivo) VALUES (?, ?, ?, ?)",
                        a.getFecha() == null ? null : Date.valueOf(a.getFecha()),
                        a.getMonto(), a.getTipo(), a.getMotivo());
            }
        };
    }

    /** Lector de lineas crudas (sin parseo); el procesador valida/limpia cada linea. */
    private ItemReader<String> rawReader(String classpathFile) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource(classpathFile));
        reader.setLineMapper(new PassThroughLineMapper());
        reader.setLinesToSkip(1);
        return reader;
    }
}
