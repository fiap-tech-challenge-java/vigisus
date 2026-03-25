package br.com.fiap.vigisus.domain.encaminhamento;

import org.springframework.stereotype.Component;

@Component
public class ClassificacaoPressaoSusPolicy {

    private static final double TAXA_INTERNACAO_ESTIMADA = 0.05;

    public String classificar(long totalRecente, int leitosSus) {
        if (totalRecente <= 0 || leitosSus <= 0) {
            return "NORMAL";
        }

        double internacaoEstimada = totalRecente * TAXA_INTERNACAO_ESTIMADA;
        double ocupacaoEstimada = internacaoEstimada / leitosSus;

        if (ocupacaoEstimada < 0.5) {
            return "NORMAL";
        }
        if (ocupacaoEstimada < 0.75) {
            return "ELEVADA";
        }
        if (ocupacaoEstimada < 0.9) {
            return "ALTA";
        }
        return "CRITICA";
    }
}
