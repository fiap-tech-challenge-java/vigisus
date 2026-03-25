package br.com.fiap.vigisus.domain.risco;

public record MetricasRiscoClimatico(
        double temperaturaAtual,
        int umidadeAtual,
        double temperaturaMedia14Dias,
        double chuvaTotal14Dias,
        double probabilidadeMediaChuva14Dias
) {
}
