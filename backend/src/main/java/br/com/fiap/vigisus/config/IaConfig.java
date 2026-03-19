package br.com.fiap.vigisus.config;

import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.IaServiceFallback;
import br.com.fiap.vigisus.service.IaServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class IaConfig {

    @Value("${vigisus.ia.api-key:sk-fake-key-for-dev}")
    private String apiKey;

    @Value("${vigisus.ia.model:gpt-4o-mini}")
    private String model;

    @Value("${vigisus.ia.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${vigisus.ia.max-tokens:500}")
    private int maxTokens;

    @Value("${vigisus.ia.temperature:0.3}")
    private double temperature;

    private static final String FAKE_KEY_PREFIX = "sk-fake";

    @Bean
    public IaService iaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith(FAKE_KEY_PREFIX)) {
            return new IaServiceImpl(apiKey, model, apiUrl, maxTokens, temperature,
                    restTemplate, objectMapper);
        }
        log.warn("AVISO: OPENAI_API_KEY não configurada — usando fallback");
        return new IaServiceFallback();
    }
}
