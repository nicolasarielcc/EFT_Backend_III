package cl.duoc.banco.bff.cuentas.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Evento de transaccion completada (producido por ms-pagos en el topic
 * "transacciones"). ms-cuentas lo consume para actualizar el saldo.
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TransaccionEvent {

    private Long cuentaId;
    private String tipo;
    private BigDecimal monto;
    private String fecha;
    private String descripcion;
}
