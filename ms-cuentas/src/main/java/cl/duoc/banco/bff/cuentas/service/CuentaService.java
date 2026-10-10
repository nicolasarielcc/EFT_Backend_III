package cl.duoc.banco.bff.cuentas.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.banco.bff.cuentas.config.KafkaProducerConfig;
import cl.duoc.banco.bff.cuentas.dto.CuentaDTO;
import cl.duoc.banco.bff.cuentas.dto.CuentaRequest;
import cl.duoc.banco.bff.cuentas.dto.RetiroEvent;
import cl.duoc.banco.bff.cuentas.dto.RetiroResultDTO;
import cl.duoc.banco.bff.cuentas.dto.SaldoDTO;
import cl.duoc.banco.bff.cuentas.entity.Cuenta;
import cl.duoc.banco.bff.cuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reglas de negocio de cuentas: apertura, cierre, mantenimiento, saldo y retiro.
 */
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final KafkaTemplate<String, RetiroEvent> kafkaTemplate;

    public List<CuentaDTO> listar() {
        return cuentaRepository.findAll().stream().map(CuentaDTO::from).toList();
    }

    public CuentaDTO obtener(Long cuentaId) {
        return CuentaDTO.from(buscarCuenta(cuentaId));
    }

    public SaldoDTO saldo(Long cuentaId) {
        Cuenta cuenta = buscarCuenta(cuentaId);
        return new SaldoDTO(cuenta.getCuentaId(), cuenta.getSaldo());
    }

    @Transactional
    public CuentaDTO aperturar(CuentaRequest request) {
        if (cuentaRepository.existsById(request.cuentaId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe la cuenta " + request.cuentaId());
        }
        Cuenta cuenta = cuentaRepository.save(new Cuenta(
                request.cuentaId(), request.nombre(), request.saldo(), request.edad(), request.tipo()));
        return CuentaDTO.from(cuenta);
    }

    @Transactional
    public CuentaDTO mantener(Long cuentaId, CuentaRequest request) {
        Cuenta cuenta = buscarCuenta(cuentaId);
        cuenta.setNombre(request.nombre());
        cuenta.setEdad(request.edad());
        cuenta.setTipo(request.tipo());
        cuentaRepository.save(cuenta);
        return CuentaDTO.from(cuenta);
    }

    @Transactional
    public void cerrar(Long cuentaId) {
        Cuenta cuenta = buscarCuenta(cuentaId);
        cuentaRepository.delete(cuenta);
    }

    @Transactional
    public RetiroResultDTO retirar(Long cuentaId, BigDecimal monto) {
        Cuenta cuenta = buscarCuenta(cuentaId);

        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        publicarEventoRetiro(cuentaId, monto);

        return new RetiroResultDTO(cuentaId, "APROBADO", monto, cuenta.getSaldo());
    }

    private Cuenta buscarCuenta(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la cuenta con id " + cuentaId));
    }

    private void publicarEventoRetiro(Long cuentaId, BigDecimal monto) {
        try {
            RetiroEvent evento = new RetiroEvent(cuentaId, monto, "APROBADO", Instant.now().toString());
            kafkaTemplate.send(KafkaProducerConfig.TOPIC, evento)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            System.err.println("No se pudo publicar el evento de retiro: " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error al publicar evento de retiro: " + e.getMessage());
        }
    }
}
