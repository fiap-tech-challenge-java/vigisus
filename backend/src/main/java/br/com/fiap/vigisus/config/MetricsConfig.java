package br.com.fiap.vigisus.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuração de métricas customizadas do VigiSUS.
 * Coletadas via Micrometer e expostas no Actuator (porta 9090).
 */
@Configuration
public class MetricsConfig {
    // Beans de counters podem ser criados aqui se necessário.
    // Os counters principais são criados inline nos services via MeterRegistry.
}
