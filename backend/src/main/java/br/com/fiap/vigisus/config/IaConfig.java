package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.IaServiceGeminiImpl;
import br.com.fiap.vigisus.service.IaServiceFallback;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class IaConfig {

    @Value("${vigisus.ia.api-key:}")
    private String apiKey;

    @Value("${vigisus.ia.model:gemini-2.5-flash}")
    private String model;

    @Bean
    public IaService iaService(Client geminiClient) {
        // geminiClient vem do GeminiConfig
        // Se a chave estiver configurada → usa Gemini real
        // Se não → usa fallback de texto (não quebra o sistema)
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("[IaConfig] Usando Gemini (modelo: {})", model);
            return new IaServiceGeminiImpl(geminiClient, model);
        }
        log.warn("[IaConfig] GEMINI_API_KEY ausente — usando fallback de texto");
        log.warn("[IaConfig] Para IA real: adicione GEMINI_API_KEY no .env");
        return new IaServiceFallback();
    }
}
