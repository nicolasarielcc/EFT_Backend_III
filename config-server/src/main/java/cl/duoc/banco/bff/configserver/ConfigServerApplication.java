package cl.duoc.banco.bff.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server del Banco XYZ (EFT).
 *
 * <p>Centraliza las configuraciones de todos los microservicios. Los clientes
 * importan su configuracion con
 * {@code spring.config.import=optional:configserver:http://config-server:8888}.</p>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
