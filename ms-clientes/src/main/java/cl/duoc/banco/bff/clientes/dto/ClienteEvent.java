package cl.duoc.banco.bff.clientes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Evento de cliente publicado en el topic "clientes" (Kafka).
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ClienteEvent {

    private Long id;
    private String nombre;
    private String accion;
    private String fecha;
}
