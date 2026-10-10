package cl.duoc.banco.bff.clientes.dto;

import cl.duoc.banco.bff.clientes.entity.Cliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion de creacion/actualizacion de cliente.
 */
public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email invalido") String email,
        String telefono,
        String direccion) {
}
