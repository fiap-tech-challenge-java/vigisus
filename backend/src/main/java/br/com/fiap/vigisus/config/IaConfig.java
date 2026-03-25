package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.IaServiceFallback;
import br.com.fiap.vigisus.service.IaServiceGeminiImpl;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
    public IaService iaService(ObjectProvider<Client> geminiClientProvider) {
        if (apiKey != null && !apiKey.isBlank()) {
            Client geminiClient = geminiClientProvider.getIfAvailable();
            if (geminiClient == null) {
                throw new IllegalStateException("Cliente Gemini indisponivel apesar de API key configurada.");
            }
            log.info("[IaConfig] Usando Gemini (modelo: {})", model);
            return new IaServiceGeminiImpl(geminiClient, model);
        }

        log.warn("[IaConfig] GEMINI_API_KEY ausente - usando fallback de texto");
        log.warn("[IaConfig] Para IA real: adicione GEMINI_API_KEY no .env");
        return new IaServiceFallback();
    }
}
