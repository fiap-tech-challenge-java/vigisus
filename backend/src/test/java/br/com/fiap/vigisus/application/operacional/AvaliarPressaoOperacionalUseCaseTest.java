package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.service.PressaoOperacionalService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvaliarPressaoOperacionalUseCaseTest {

    @Test
    void executar_delegaAoService() {
        PressaoOperacionalService service = mock(PressaoOperacionalService.class);
        AvaliarPressaoOperacionalUseCase useCase = new AvaliarPressaoOperacionalUseCase(service);
        PressaoOperacionalRequest request = new PressaoOperacionalRequest("3131307", 5, "UPA");
        PressaoOperacionalResponse response = PressaoOperacionalResponse.builder().nivelAtencao("ELEVADO").build();
        when(service.avaliarPressao(request)).thenReturn(response);

        assertThat(useCase.executar(request)).isEqualTo(response);
    }
}
