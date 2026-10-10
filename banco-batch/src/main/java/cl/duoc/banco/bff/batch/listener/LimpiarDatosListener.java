package cl.duoc.banco.bff.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Antes de cada ejecucion de un job, limpia sus tablas asociadas (hace el batch
 * idempotente: re-ejecutar un job reproduce el mismo estado sin duplicar datos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpiarDatosListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        switch (jobName) {
            case "reporteTransaccionesJob" -> {
                jdbcTemplate.update("DELETE FROM resumen_transacciones");
                jdbcTemplate.update("DELETE FROM anomalia");
                jdbcTemplate.update("DELETE FROM movimiento");
            }
            case "calculoInteresesJob" -> jdbcTemplate.update("DELETE FROM cuenta");
            case "estadosAnualesJob" -> jdbcTemplate.update("DELETE FROM transaccion");
            default -> log.info("Job {} no tiene tablas a limpiar", jobName);
        }
        log.info("Datos previos de {} eliminados", jobName);
    }
}
