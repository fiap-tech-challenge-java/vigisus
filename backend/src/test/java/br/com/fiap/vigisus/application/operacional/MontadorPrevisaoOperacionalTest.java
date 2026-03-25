package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MontadorPrevisaoOperacionalTest {

    private final MontadorPrevisaoOperacional montador = new MontadorPrevisaoOperacional();

    @Test
    void montar_quandoRiscoNulo_retornaFallback() {
        PrevisaoProximosDiasDTO previsao = montador.montar(null);

        assertThat(previsao.getRiscoClimatico()).contains("Indispon");
        assertThat(previsao.getTendencia7Dias()).contains("dispon");
    }

    @Test
    void montar_quandoRiscoComFatores_montaResumo() {
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder()
                .coIbge("3131307")
                .municipio("Lavras")
                .score(7)
                .classificacao("MUITO_ALTO")
                .fatores(List.of("Temperatura alta", "Chuva intensa"))
                .build();

        PrevisaoProximosDiasDTO previsao = montador.montar(risco);

        assertThat(previsao.getRiscoClimatico()).contains("Score 7/8").contains("MUITO_ALTO");
        assertThat(previsao.getTendencia7Dias()).contains("Temperatura alta").contains("Chuva intensa");
    }
}
