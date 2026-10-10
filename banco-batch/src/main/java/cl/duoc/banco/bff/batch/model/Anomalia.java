package cl.duoc.banco.bff.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anomalia detectada en el Reporte de Transacciones Diarias
 * (monto negativo/cero, fecha invalida, tipo desconocido, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anomalia {

    private Long id;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
    private String motivo;
}
