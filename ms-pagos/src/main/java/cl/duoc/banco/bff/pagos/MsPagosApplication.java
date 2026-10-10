package cl.duoc.banco.bff.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Procesamiento de Pagos del Banco XYZ (EFT).
 *
 * <p>Procesa pagos, transferencias y depositos, publicando eventos de
 * "transacciones completadas" en Kafka. Se registra en Eureka y lee
 * configuracion desde el Config Server.</p>
 */
@SpringBootApplication(scanBasePackages = "cl.duoc.banco.bff")
public class MsPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPagosApplication.class, args);
    }
}
