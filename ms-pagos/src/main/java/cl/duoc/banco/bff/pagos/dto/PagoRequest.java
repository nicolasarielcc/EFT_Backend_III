package cl.duoc.banco.bff.pagos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Peticion de pago/transferencia/deposito.
 */
public record PagoRequest(
        @NotNull(message = "El numero de cuenta es obligatorio") @Positive Long cuentaId,
        @NotBlank(message = "El tipo es obligatorio") String tipo,
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
        String descripcion) {
}
