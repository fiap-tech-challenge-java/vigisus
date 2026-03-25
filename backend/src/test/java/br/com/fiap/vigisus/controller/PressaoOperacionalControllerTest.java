package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.service.PressaoOperacionalService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PressaoOperacionalControllerTest {

    @Test
    void avaliarPressao_delegaAoService() {
        PressaoOperacionalService service = mock(PressaoOperacionalService.class);
        PressaoOperacionalController controller = new PressaoOperacionalController(service);
        PressaoOperacionalRequest request = new PressaoOperacionalRequest("3131307", 5, "UPA");
        PressaoOperacionalResponse response = PressaoOperacionalResponse.builder().nivelAtencao("ELEVADO").build();
        when(service.avaliarPressao(request)).thenReturn(response);

        assertThat(controller.avaliarPressao(request)).isEqualTo(response);
    }

    @Test
    void getProtocoloSurto_retornaEstruturaEsperada() {
        PressaoOperacionalService service = mock(PressaoOperacionalService.class);
        PressaoOperacionalController controller = new PressaoOperacionalController(service);

        Map<String, Object> response = controller.getProtocoloSurto();

        assertThat(response).containsKey("titulo");
        assertThat((List<?>) response.get("passos")).isNotEmpty();
        assertThat(((Map<?, ?>) response.get("contatos")).containsKey("vigilancia_epidemiologica")).isTrue();
    }
}
