package cl.duoc.banco.bff.pagos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Evento de transaccion completada publicado en el topic "transacciones".
 * El microservicio de cuentas lo consume para actualizar el saldo (consistencia
 * eventual en un entorno distribuido).
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
