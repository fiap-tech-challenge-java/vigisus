package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.triagem.AvaliarTriagemUseCase;
import br.com.fiap.vigisus.application.triagem.ConsultarCatalogoTriagemUseCase;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriagemControllerTest {

    @Test
    void avaliar_delegaAoUseCase() {
        AvaliarTriagemUseCase avaliarTriagemUseCase = mock(AvaliarTriagemUseCase.class);
        ConsultarCatalogoTriagemUseCase consultarCatalogoTriagemUseCase = mock(ConsultarCatalogoTriagemUseCase.class);
        TriagemController controller = new TriagemController(avaliarTriagemUseCase, consultarCatalogoTriagemUseCase);
        TriagemRequest request = TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of("febre"))
                .diasSintomas(2)
                .idade(20)
                .comorbidades(List.of())
                .build();
        TriagemResponse response = TriagemResponse.builder().prioridade("AMARELO").build();
        when(avaliarTriagemUseCase.executar(request)).thenReturn(response);

        assertThat(controller.avaliar(request)).isEqualTo(response);
    }

    @Test
    void sintomas_delegaAoUseCase() {
        AvaliarTriagemUseCase avaliarTriagemUseCase = mock(AvaliarTriagemUseCase.class);
        ConsultarCatalogoTriagemUseCase consultarCatalogoTriagemUseCase = mock(ConsultarCatalogoTriagemUseCase.class);
        TriagemController controller = new TriagemController(avaliarTriagemUseCase, consultarCatalogoTriagemUseCase);
        Map<String, List<String>> resposta = Map.of("sintomas", List.of("febre"), "comorbidades", List.of("diabetes"));
        when(consultarCatalogoTriagemUseCase.executar()).thenReturn(resposta);

        assertThat(controller.sintomas()).isEqualTo(resposta);
    }
}
