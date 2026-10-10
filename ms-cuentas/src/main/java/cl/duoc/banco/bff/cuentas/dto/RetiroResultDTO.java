package cl.duoc.banco.bff.cuentas.dto;

import java.math.BigDecimal;

public record RetiroResultDTO(
        Long cuentaId,
        String estado,
        BigDecimal montoRetirado,
        BigDecimal saldo) {
}
