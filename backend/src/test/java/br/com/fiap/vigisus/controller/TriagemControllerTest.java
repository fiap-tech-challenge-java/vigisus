package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.TriagemService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriagemControllerTest {

    @Test
    void avaliar_delegaAoService() {
        TriagemService service = mock(TriagemService.class);
        TriagemController controller = new TriagemController(service);
        TriagemRequest request = TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of("febre"))
                .diasSintomas(2)
                .idade(20)
                .comorbidades(List.of())
                .build();
        TriagemResponse response = TriagemResponse.builder().prioridade("AMARELO").build();
        when(service.avaliar(request)).thenReturn(response);

        assertThat(controller.avaliar(request)).isEqualTo(response);
    }

    @Test
    void sintomas_retornaCatalogosFixos() {
        TriagemService service = mock(TriagemService.class);
        TriagemController controller = new TriagemController(service);

        Map<String, List<String>> resposta = controller.sintomas();

        assertThat(resposta).containsKeys("sintomas", "comorbidades");
        assertThat(resposta.get("sintomas")).contains("febre");
        assertThat(resposta.get("comorbidades")).contains("diabetes");
    }
}
