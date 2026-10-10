package cl.duoc.banco.bff.clientes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import cl.duoc.banco.bff.common.security.JwtAuthConverter;
import lombok.RequiredArgsConstructor;

/**
 * Seguridad del microservicio de clientes (resource server OAuth 2.0):
 * - Lecturas: requieren token valido (cualquier canal autenticado).
 * - Altas/modificaciones: solo el canal Web (ROLE_WEB).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasAuthority("ROLE_WEB")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**").hasAuthority("ROLE_WEB")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasAuthority("ROLE_WEB")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }
}
