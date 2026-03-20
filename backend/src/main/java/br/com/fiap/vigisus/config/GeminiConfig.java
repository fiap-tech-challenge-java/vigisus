package br.com.fiap.vigisus.config;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GeminiConfig {

    @Value("${vigisus.ia.api-key:}")
    private String apiKey;

    @Bean
    public Client geminiClient() {
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("[GeminiConfig] API key configurada via application.yml");
            return Client.builder().apiKey(apiKey).build();
        }
        log.warn("[GeminiConfig] GEMINI_API_KEY não encontrada — IA usará fallback");
        return new Client();
    }
}
