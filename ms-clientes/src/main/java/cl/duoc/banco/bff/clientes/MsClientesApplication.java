package cl.duoc.banco.bff.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Gestion de Clientes del Banco XYZ (EFT).
 *
 * <p>Administra la informacion personal y los perfiles de los clientes.
 * Se registra en Eureka y lee configuracion desde el Config Server.</p>
 */
@SpringBootApplication(scanBasePackages = "cl.duoc.banco.bff")
public class MsClientesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsClientesApplication.class, args);
    }
}
