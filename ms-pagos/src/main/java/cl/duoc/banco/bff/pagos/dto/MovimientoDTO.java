package cl.duoc.banco.bff.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.banco.bff.pagos.entity.Movimiento;

public record MovimientoDTO(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {

    public static MovimientoDTO from(Movimiento m) {
        return new MovimientoDTO(m.getId(), m.getFecha(), m.getMonto(), m.getTipo());
    }
}
