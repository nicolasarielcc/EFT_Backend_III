package cl.duoc.banco.bff.cuentas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.banco.bff.cuentas.dto.CuentaDTO;
import cl.duoc.banco.bff.cuentas.dto.CuentaRequest;
import cl.duoc.banco.bff.cuentas.dto.RetiroRequest;
import cl.duoc.banco.bff.cuentas.dto.RetiroResultDTO;
import cl.duoc.banco.bff.cuentas.dto.SaldoDTO;
import cl.duoc.banco.bff.cuentas.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API de gestion de cuentas (apertura, cierre, mantenimiento, saldo y retiro).
 */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping
    public List<CuentaDTO> listarCuentas() {
        return cuentaService.listar();
    }

    @GetMapping("/{cuentaId}")
    public CuentaDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return cuentaService.obtener(cuentaId);
    }

    @GetMapping("/{cuentaId}/saldo")
    public SaldoDTO consultarSaldo(@PathVariable Long cuentaId) {
        return cuentaService.saldo(cuentaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDTO aperturar(@Valid @RequestBody CuentaRequest request) {
        return cuentaService.aperturar(request);
    }

    @PutMapping("/{cuentaId}")
    public CuentaDTO mantener(@PathVariable Long cuentaId, @Valid @RequestBody CuentaRequest request) {
        return cuentaService.mantener(cuentaId, request);
    }

    @DeleteMapping("/{cuentaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@PathVariable Long cuentaId) {
        cuentaService.cerrar(cuentaId);
    }

    @PostMapping("/{cuentaId}/retiros")
    public RetiroResultDTO retirar(@PathVariable Long cuentaId, @Valid @RequestBody RetiroRequest request) {
        return cuentaService.retirar(cuentaId, request.monto());
    }
}
