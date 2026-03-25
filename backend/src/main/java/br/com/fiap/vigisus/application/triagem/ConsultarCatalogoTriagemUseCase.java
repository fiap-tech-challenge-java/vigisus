package br.com.fiap.vigisus.application.triagem;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConsultarCatalogoTriagemUseCase {

    public Map<String, List<String>> executar() {
        return Map.of(
                "sintomas", List.of(
                        "febre", "dor_muscular", "dor_retro_orbital", "cefaleia",
                        "exantema", "nausea", "vomito", "dor_abdominal",
                        "sangramento", "prostacao", "tontura", "falta_ar"
                ),
                "comorbidades", List.of(
                        "diabetes", "hipertensao", "obesidade", "gestante",
                        "doenca_renal", "doenca_cardiaca", "imunossupressao"
                )
        );
    }
}
