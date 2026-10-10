package cl.duoc.banco.bff.clientes.dto;

import cl.duoc.banco.bff.clientes.entity.Cliente;

/**
 * Respuesta de cliente.
 */
public record ClienteDTO(
        Long id,
        String nombre,
        String email,
        String telefono,
        String direccion) {

    public static ClienteDTO from(Cliente c) {
        return new ClienteDTO(c.getId(), c.getNombre(), c.getEmail(), c.getTelefono(), c.getDireccion());
    }
}
