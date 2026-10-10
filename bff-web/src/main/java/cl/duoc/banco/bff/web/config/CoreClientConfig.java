package cl.duoc.banco.bff.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el API Gateway (punto de entrada unico a los
 * microservicios). El Gateway enruta por Eureka hacia ms-clientes/ms-cuentas/
 * ms-pagos. El BFF reenvia el token JWT del cliente (relay) en cada llamada.
 */
@Configuration
public class CoreClientConfig {

    @Bean
    public RestClient coreRestClient(@Value("${gateway.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
