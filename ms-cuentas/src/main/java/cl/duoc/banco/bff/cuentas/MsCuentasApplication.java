package cl.duoc.banco.bff.cuentas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Gestion de Cuentas del Banco XYZ (EFT).
 *
 * <p>Maneja apertura, cierre y mantenimiento de cuentas, consulta de saldo y
 * retiros. Se registra en Eureka y lee configuracion desde el Config Server.</p>
 */
@SpringBootApplication(scanBasePackages = "cl.duoc.banco.bff")
public class MsCuentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCuentasApplication.class, args);
    }
}
