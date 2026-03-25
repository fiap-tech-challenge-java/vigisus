package br.com.fiap.vigisus.domain.operacional;

import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChecklistOperacionalPolicyTest {

    private final ChecklistOperacionalPolicy policy = new ChecklistOperacionalPolicy();

    private final ContextoEpidemiologicoDTO contexto = ContextoEpidemiologicoDTO.builder()
            .classificacaoAtual("EPIDEMIA")
            .casosUltimasSemanas(120)
            .tendencia("CRESCENTE")
            .comparativoHistorico("Comparando...")
            .build();

    private final PrevisaoProximosDiasDTO previsao = PrevisaoProximosDiasDTO.builder()
            .riscoClimatico("Score 7/8")
            .tendencia7Dias("Fatores de risco")
            .build();

    @Test
    void montarChecklist_quandoCritico_retornaChecklistCompleto() {
        List<String> checklist = policy.montarChecklist("CRITICO", contexto, previsao);

        assertThat(checklist).hasSize(6);
        assertThat(checklist.get(0)).contains("surto");
        assertThat(checklist.get(2)).contains("Score 7/8");
    }

    @Test
    void montarChecklist_quandoElevado_retornaChecklistIntermediario() {
        List<String> checklist = policy.montarChecklist("ELEVADO", contexto, previsao);

        assertThat(checklist).hasSize(5);
        assertThat(checklist.get(0)).contains("suspeitas");
        assertThat(checklist.get(4)).contains("Fatores de risco");
    }

    @Test
    void montarChecklist_quandoNormal_retornaChecklistBasico() {
        List<String> checklist = policy.montarChecklist("NORMAL", contexto, previsao);

        assertThat(checklist).hasSize(3);
        assertThat(checklist.get(0)).contains("padr");
        assertThat(checklist.get(2)).contains("Score 7/8");
    }
}
