package cl.duoc.banco.bff.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka del Banco XYZ (EFT).
 *
 * <p>Registro y descubrimiento dinamico: los microservicios se registran aqui y
 * se encuentran entre si por nombre logico (sin URLs fijas).</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
