package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.IaServiceGeminiImpl;
import br.com.fiap.vigisus.service.IaServiceFallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class IaConfig {

    @Value("${vigisus.ia.api-key:}")
    private String apiKey;

    @Value("${vigisus.ia.model:gemini-2.0-flash}")
    private String model;

    @Bean
    public IaService iaService() {
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("IaService: usando Gemini (modelo: {})", model);
            return new IaServiceGeminiImpl(apiKey, model);
        }
        log.warn("IaService: GEMINI_API_KEY não configurada — usando fallback de texto");
        log.warn("Para usar IA real: adicione GEMINI_API_KEY no .env");
        return new IaServiceFallback();
    }
}
