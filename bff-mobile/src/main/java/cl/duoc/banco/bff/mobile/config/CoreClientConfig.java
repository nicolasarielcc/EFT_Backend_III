package cl.duoc.banco.bff.mobile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el API Gateway (reenvio del token JWT del canal Mobile).
 */
@Configuration
public class CoreClientConfig {

    @Bean
    public RestClient coreRestClient(@Value("${gateway.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
