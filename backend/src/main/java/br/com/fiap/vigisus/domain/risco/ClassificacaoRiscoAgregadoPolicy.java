package br.com.fiap.vigisus.domain.risco;

import org.springframework.stereotype.Component;

@Component
public class ClassificacaoRiscoAgregadoPolicy implements ClassificacaoRiscoPolicy {

    @Override
    public String classificar(int score) {
        if (score <= 1) {
            return "BAIXO";
        }
        if (score <= 3) {
            return "MODERADO";
        }
        if (score <= 5) {
            return "ALTO";
        }
        return "EPIDEMIA";
    }
}
