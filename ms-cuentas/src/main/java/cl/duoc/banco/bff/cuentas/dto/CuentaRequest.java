package cl.duoc.banco.bff.cuentas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Peticion de apertura/mantenimiento de cuenta.
 */
public record CuentaRequest(
        @NotNull(message = "El numero de cuenta es obligatorio") @Positive Long cuentaId,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull @DecimalMin(value = "0.0", message = "El saldo inicial no puede ser negativo") BigDecimal saldo,
        @NotNull Integer edad,
        @NotBlank String tipo) {
}
