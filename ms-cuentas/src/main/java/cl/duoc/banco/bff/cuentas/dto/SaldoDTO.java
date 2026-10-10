package cl.duoc.banco.bff.cuentas.dto;

import java.math.BigDecimal;

public record SaldoDTO(
        Long cuentaId,
        BigDecimal saldo) {
}
