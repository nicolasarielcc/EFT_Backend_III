package cl.duoc.banco.bff.clientes.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.banco.bff.clientes.config.KafkaProducerConfig;
import cl.duoc.banco.bff.clientes.dto.ClienteDTO;
import cl.duoc.banco.bff.clientes.dto.ClienteEvent;
import cl.duoc.banco.bff.clientes.dto.ClienteRequest;
import cl.duoc.banco.bff.clientes.entity.Cliente;
import cl.duoc.banco.bff.clientes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reglas de negocio de clientes: alta, consulta, actualizacion y baja.
 * Publica eventos de negocio en Kafka (arquitectura orientada a eventos).
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final KafkaTemplate<String, ClienteEvent> kafkaTemplate;

    public List<ClienteDTO> listar() {
        return clienteRepository.findAll().stream().map(ClienteDTO::from).toList();
    }

    public ClienteDTO obtener(Long id) {
        return ClienteDTO.from(buscar(id));
    }

    @Transactional
    public ClienteDTO crear(ClienteRequest request) {
        clienteRepository.findByEmail(request.email()).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese email");
        });

        Cliente cliente = clienteRepository.save(new Cliente(null,
                request.nombre(), request.email(), request.telefono(), request.direccion()));

        publicarEvento(cliente.getId(), cliente.getNombre(), "CREADO");
        return ClienteDTO.from(cliente);
    }

    @Transactional
    public ClienteDTO actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        cliente.setNombre(request.nombre());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());
        clienteRepository.save(cliente);

        publicarEvento(cliente.getId(), cliente.getNombre(), "ACTUALIZADO");
        return ClienteDTO.from(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        clienteRepository.delete(cliente);
        publicarEvento(cliente.getId(), cliente.getNombre(), "ELIMINADO");
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el cliente con id " + id));
    }

    private void publicarEvento(Long id, String nombre, String accion) {
        try {
            ClienteEvent evento = new ClienteEvent(id, nombre, accion, Instant.now().toString());
            kafkaTemplate.send(KafkaProducerConfig.TOPIC, evento)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            System.err.println("No se pudo publicar evento de cliente: " + ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error al publicar evento de cliente: " + e.getMessage());
        }
    }
}
