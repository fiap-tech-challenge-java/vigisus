package br.com.fiap.vigisus.config;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GeminiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "vigisus.ia", name = "api-key")
    public Client geminiClient(@Value("${vigisus.ia.api-key}") String apiKey) {
        log.info("[GeminiConfig] API key configurada via application.yml");
        return Client.builder().apiKey(apiKey).build();
    }
}
