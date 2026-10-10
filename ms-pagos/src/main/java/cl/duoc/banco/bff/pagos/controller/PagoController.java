package cl.duoc.banco.bff.pagos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.banco.bff.pagos.dto.MovimientoDTO;
import cl.duoc.banco.bff.pagos.dto.PagoRequest;
import cl.duoc.banco.bff.pagos.dto.PagoResultDTO;
import cl.duoc.banco.bff.pagos.dto.TransaccionDTO;
import cl.duoc.banco.bff.pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API de procesamiento de pagos, transferencias y depositos.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/pagos")
    public PagoResultDTO procesarPago(@Valid @RequestBody PagoRequest request) {
        return pagoService.procesar(request);
    }

    @PostMapping("/pagos/transferencias")
    public PagoResultDTO procesarTransferencia(@Valid @RequestBody PagoRequest request) {
        return pagoService.procesar(request);
    }

    @PostMapping("/pagos/depositos")
    public PagoResultDTO procesarDeposito(@Valid @RequestBody PagoRequest request) {
        return pagoService.procesar(request);
    }

    @GetMapping("/transacciones/cuenta/{cuentaId}")
    public List<TransaccionDTO> listarPorCuenta(@PathVariable Long cuentaId) {
        return pagoService.listarPorCuenta(cuentaId);
    }

    @GetMapping("/movimientos")
    public List<MovimientoDTO> listarMovimientos() {
        return pagoService.listarMovimientos();
    }
}
