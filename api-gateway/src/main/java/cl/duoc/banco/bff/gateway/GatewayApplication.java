package cl.duoc.banco.bff.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway del Banco XYZ (EFT).
 *
 * <p>Punto de entrada unico al sistema: enruta y balancea las solicitudes hacia
 * los microservicios (ms-clientes, ms-cuentas, ms-pagos) usando Eureka para la
 * deteccion dinamica de servicios ({@code lb://}).</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
