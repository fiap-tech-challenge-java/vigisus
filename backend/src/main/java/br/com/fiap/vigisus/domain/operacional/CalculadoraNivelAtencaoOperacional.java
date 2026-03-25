package br.com.fiap.vigisus.domain.operacional;

import org.springframework.stereotype.Component;

@Component
public class CalculadoraNivelAtencaoOperacional {

    public String calcular(int suspeitasDia, String classificacao, String tendencia, String classificacaoRiscoClimatico) {
        int score = 0;

        if (suspeitasDia >= 10) {
            score += 3;
        } else if (suspeitasDia >= 5) {
            score += 2;
        } else if (suspeitasDia >= 2) {
            score += 1;
        }

        switch (classificacao) {
            case "EPIDEMIA" -> score += 3;
            case "ALTO" -> score += 2;
            case "MODERADO" -> score += 1;
            default -> {
            }
        }

        if ("CRESCENTE".equals(tendencia)) {
            score += 2;
        }

        if ("MUITO_ALTO".equals(classificacaoRiscoClimatico)) {
            score += 2;
        } else if ("ALTO".equals(classificacaoRiscoClimatico)) {
            score += 1;
        }

        if (score <= 2) {
            return "NORMAL";
        }
        if (score <= 5) {
            return "ELEVADO";
        }
        return "CRITICO";
    }
}
