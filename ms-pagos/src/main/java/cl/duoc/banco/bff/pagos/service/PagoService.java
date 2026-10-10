package cl.duoc.banco.bff.pagos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.banco.bff.pagos.config.KafkaProducerConfig;
import cl.duoc.banco.bff.pagos.dto.MovimientoDTO;
import cl.duoc.banco.bff.pagos.dto.PagoRequest;
import cl.duoc.banco.bff.pagos.dto.PagoResultDTO;
import cl.duoc.banco.bff.pagos.dto.TransaccionDTO;
import cl.duoc.banco.bff.pagos.dto.TransaccionEvent;
import cl.duoc.banco.bff.pagos.entity.Transaccion;
import cl.duoc.banco.bff.pagos.repository.MovimientoRepository;
import cl.duoc.banco.bff.pagos.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reglas de negocio de pagos: procesa pagos, transferencias y depositos.
 * Registra la transaccion y publica un evento de "transaccion completada".
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("pago", "transferencia", "deposito");

    private final TransaccionRepository transaccionRepository;
    private final MovimientoRepository movimientoRepository;
    private final KafkaTemplate<String, TransaccionEvent> kafkaTemplate;

    @Transactional
    public PagoResultDTO procesar(PagoRequest request) {
        String tipo = request.tipo() == null ? "" : request.tipo().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo invalido. Valores validos: pago, transferencia, deposito");
        }

        Transaccion transaccion = transaccionRepository.save(new Transaccion(
                null, request.cuentaId(), LocalDate.now(), tipo,
                request.monto(), request.descripcion()));

        publicarEventoTransaccion(transaccion);

        return new PagoResultDTO(request.cuentaId(), tipo, request.monto(), "PROCESADO");
    }

    public List<TransaccionDTO> listarPorCuenta(Long cuentaId) {
        return transaccionRepository.findByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .map(TransaccionDTO::from)
                .toList();
    }

    public List<MovimientoDTO> listarMovimientos() {
        return movimientoRepository.findAllByOrderByFechaDesc().stream()
                .map(MovimientoDTO::from)
                .toList();
    }

    private void publicarEventoTransaccion(Transaccion t) {
        try {
            TransaccionEvent evento = new TransaccionEvent(
                    t.getCuentaId(), t.getTipo(), t.getMonto(),
                    Instant.now().toString(), t.getDescripcion());
            kafkaTemplate.send(KafkaProducerConfig.TOPIC, evento)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            System.err.println("No se pudo publicar evento de transaccion: " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error al publicar evento de transaccion: " + e.getMessage());
        }
    }
}
