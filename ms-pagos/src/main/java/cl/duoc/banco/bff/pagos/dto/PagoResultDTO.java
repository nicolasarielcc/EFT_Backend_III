package cl.duoc.banco.bff.pagos.dto;

import java.math.BigDecimal;

public record PagoResultDTO(
        Long cuentaId,
        String tipo,
        BigDecimal monto,
        String estado) {
}
