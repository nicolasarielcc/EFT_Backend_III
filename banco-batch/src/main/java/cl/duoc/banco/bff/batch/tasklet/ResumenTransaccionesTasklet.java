package cl.duoc.banco.bff.batch.tasklet;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera el resumen del Reporte de Transacciones Diarias: total de
 * transacciones validas, total de anomalias y monto total procesado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumenTransaccionesTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Integer totalTransacciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movimiento", Integer.class);
        Integer totalAnomalias = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM anomalia", Integer.class);
        java.math.BigDecimal montoTotal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(monto), 0) FROM movimiento", java.math.BigDecimal.class);

        jdbcTemplate.update(
                "INSERT INTO resumen_transacciones (fecha, total_transacciones, total_anomalias, monto_total) "
                        + "VALUES (?, ?, ?, ?)",
                LocalDate.now(), totalTransacciones, totalAnomalias, montoTotal);

        log.info("Resumen generado: transacciones={}, anomalias={}, montoTotal={}",
                totalTransacciones, totalAnomalias, montoTotal);

        return RepeatStatus.FINISHED;
    }
}
