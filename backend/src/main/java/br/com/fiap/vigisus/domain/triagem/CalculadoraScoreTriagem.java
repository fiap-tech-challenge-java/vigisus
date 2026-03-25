package br.com.fiap.vigisus.domain.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class CalculadoraScoreTriagem {

    private static final Set<String> SINTOMAS_ALARME = Set.of(
            "dor_abdominal", "sangramento", "vomito", "prostacao", "falta_ar"
    );

    private static final Set<String> SINTOMAS_CLASSICOS = Set.of(
            "febre", "dor_muscular", "dor_retro_orbital", "cefaleia",
            "exantema", "nausea", "tontura"
    );

    private static final Set<String> COMORBIDADES_RISCO = Set.of(
            "diabetes", "gestante", "doenca_renal", "doenca_cardiaca", "imunossupressao"
    );

    private static final Set<String> COMORBIDADES_MODERADAS = Set.of(
            "hipertensao", "obesidade"
    );

    public double calcular(TriagemRequest request) {
        List<String> sintomas = request.getSintomas() != null ? request.getSintomas() : List.of();
        List<String> comorbidades = request.getComorbidades() != null ? request.getComorbidades() : List.of();

        double score = 0;

        for (String sintoma : sintomas) {
            if (SINTOMAS_ALARME.contains(sintoma)) {
                score += 3;
            }
        }

        for (String sintoma : sintomas) {
            if (SINTOMAS_CLASSICOS.contains(sintoma)) {
                score += 1;
            }
        }

        for (String comorbidade : comorbidades) {
            if (COMORBIDADES_RISCO.contains(comorbidade)) {
                score += 2;
            }
        }

        for (String comorbidade : comorbidades) {
            if (COMORBIDADES_MODERADAS.contains(comorbidade)) {
                score += 1;
            }
        }

        if (request.getIdade() < 2 || request.getIdade() > 65) {
            score += 2;
        }

        if (request.getDiasSintomas() >= 5) {
            score += 1;
        }

        return score;
    }
}
