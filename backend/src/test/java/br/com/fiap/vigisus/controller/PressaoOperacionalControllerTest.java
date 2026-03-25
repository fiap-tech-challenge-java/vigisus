package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.operacional.AvaliarPressaoOperacionalUseCase;
import br.com.fiap.vigisus.application.operacional.ConsultarProtocoloSurtoUseCase;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PressaoOperacionalControllerTest {

    @Test
    void avaliarPressao_delegaAoUseCase() {
        AvaliarPressaoOperacionalUseCase avaliarPressaoOperacionalUseCase = mock(AvaliarPressaoOperacionalUseCase.class);
        ConsultarProtocoloSurtoUseCase consultarProtocoloSurtoUseCase = mock(ConsultarProtocoloSurtoUseCase.class);
        PressaoOperacionalController controller =
                new PressaoOperacionalController(avaliarPressaoOperacionalUseCase, consultarProtocoloSurtoUseCase);
        PressaoOperacionalRequest request = new PressaoOperacionalRequest("3131307", 5, "UPA");
        PressaoOperacionalResponse response = PressaoOperacionalResponse.builder().nivelAtencao("ELEVADO").build();
        when(avaliarPressaoOperacionalUseCase.executar(request)).thenReturn(response);

        assertThat(controller.avaliarPressao(request)).isEqualTo(response);
    }

    @Test
    void getProtocoloSurto_delegaAoUseCase() {
        AvaliarPressaoOperacionalUseCase avaliarPressaoOperacionalUseCase = mock(AvaliarPressaoOperacionalUseCase.class);
        ConsultarProtocoloSurtoUseCase consultarProtocoloSurtoUseCase = mock(ConsultarProtocoloSurtoUseCase.class);
        PressaoOperacionalController controller =
                new PressaoOperacionalController(avaliarPressaoOperacionalUseCase, consultarProtocoloSurtoUseCase);
        Map<String, Object> response = Map.of(
                "titulo", "Protocolo",
                "passos", List.of("1. Fazer algo"),
                "contatos", Map.of("vigilancia_epidemiologica", "0800")
        );
        when(consultarProtocoloSurtoUseCase.executar()).thenReturn(response);

        assertThat(controller.getProtocoloSurto()).isEqualTo(response);
    }
}
