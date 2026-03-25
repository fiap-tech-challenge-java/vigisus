package br.com.fiap.vigisus.domain.risco;

import org.springframework.stereotype.Component;

@Component
public class ClassificacaoRiscoMunicipioPolicy implements ClassificacaoRiscoPolicy {

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
        return "MUITO_ALTO";
    }
}
