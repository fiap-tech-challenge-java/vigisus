package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import org.springframework.stereotype.Component;

@Component
public class MontadorPrevisaoOperacional {

    public PrevisaoProximosDiasDTO montar(PrevisaoRiscoResponse risco) {
        if (risco == null) {
            return PrevisaoProximosDiasDTO.builder()
                    .riscoClimatico("IndisponÃƒÂ­vel")
                    .tendencia7Dias("PrevisÃƒÂ£o climÃƒÂ¡tica nÃƒÂ£o disponÃƒÂ­vel no momento.")
                    .build();
        }

        String riscoClimatico = String.format("Score %d/8 Ã¢â‚¬â€ %s", risco.getScore(), risco.getClassificacao());
        String tendencia7Dias = (risco.getFatores() != null && !risco.getFatores().isEmpty())
                ? "Fatores de risco identificados: " + String.join("; ", risco.getFatores())
                : "Sem fatores de risco climÃƒÂ¡tico relevantes identificados.";

        return PrevisaoProximosDiasDTO.builder()
                .riscoClimatico(riscoClimatico)
                .tendencia7Dias(tendencia7Dias)
                .build();
    }
}
