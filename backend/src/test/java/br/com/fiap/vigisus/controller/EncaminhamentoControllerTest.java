package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.encaminhamento.ConsultarEncaminhamentoUseCase;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncaminhamentoControllerTest {

    @Test
    void getEncaminhamento_delegaAoUseCase() {
        ConsultarEncaminhamentoUseCase consultarEncaminhamentoUseCase = mock(ConsultarEncaminhamentoUseCase.class);
        EncaminhamentoController controller = new EncaminhamentoController(consultarEncaminhamentoUseCase);
        EncaminhamentoResponse response = EncaminhamentoResponse.builder().tpLeito("81").build();
        when(consultarEncaminhamentoUseCase.executar("3131307", "dengue", "grave", "81", 2)).thenReturn(response);

        EncaminhamentoResponse result =
                controller.getEncaminhamento("3131307", "dengue", "grave", "81", 2);

        assertThat(result).isEqualTo(response);
    }
}
