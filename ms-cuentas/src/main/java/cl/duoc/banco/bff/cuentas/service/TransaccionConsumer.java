package cl.duoc.banco.bff.cuentas.service;

import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.banco.bff.cuentas.config.KafkaConsumerConfig;
import cl.duoc.banco.bff.cuentas.dto.TransaccionEvent;
import cl.duoc.banco.bff.cuentas.entity.Cuenta;
import cl.duoc.banco.bff.cuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor de eventos de transacciones completadas (emitidos por ms-pagos).
 *
 * <p>Actualiza el saldo de la cuenta de forma asincrona, lo que garantiza la
 * consistencia eventual de datos en un entorno distribuido:
 * <ul>
 *   <li>{@code deposito} -> incrementa el saldo.</li>
 *   <li>{@code pago} / {@code transferencia} -> decrementa el saldo.</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionConsumer {

    private static final Set<String> TIPOS_CREDITO = Set.of("deposito");

    private final CuentaRepository cuentaRepository;

    @KafkaListener(id = "transaccionListener", topics = KafkaConsumerConfig.TOPIC,
            groupId = KafkaConsumerConfig.GROUP_ID)
    @Transactional
    public void consumir(TransaccionEvent evento) {
        cuentaRepository.findById(evento.getCuentaId()).ifPresentOrElse(cuenta -> {
            if (TIPOS_CREDITO.contains(evento.getTipo())) {
                cuenta.setSaldo(cuenta.getSaldo().add(evento.getMonto()));
            } else {
                cuenta.setSaldo(cuenta.getSaldo().subtract(evento.getMonto()));
            }
            cuentaRepository.save(cuenta);
            log.info("Saldo actualizado por evento {}: cuenta {} nuevo saldo {}",
                    evento.getTipo(), evento.getCuentaId(), cuenta.getSaldo());
        }, () -> log.warn("Evento ignorado: no existe la cuenta {}", evento.getCuentaId()));
    }
}
