package br.com.fiap.vigisus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da API VigiSUS.
 *
 * ESCOPO ATUAL (Hackathon):
 * A API expõe dados epidemiológicos públicos do SUS (DATASUS, IBGE, Open-Meteo).
 * Por serem dados abertos, todos os endpoints de consulta são públicos.
 * Não há dados pessoais de pacientes — apenas estatísticas agregadas.
 *
 * NOTA: /actuator/** e /admin/** são servidos exclusivamente na porta 9090
 * (management.server.port), portanto não precisam de regras aqui.
 *
 * ROADMAP v2.0:
 * Endpoints administrativos receberão autenticação JWT para operadores de saúde pública.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configure(http))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
