package br.com.fiap.vigisus.application.encaminhamento;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarEncaminhamentoUseCaseTest {

    @Test
    void executar_usaTpLeitoExplicitoQuandoInformado() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        ConsultarEncaminhamentoUseCase useCase = new ConsultarEncaminhamentoUseCase(service, new SimpleMeterRegistry());
        EncaminhamentoResponse response = EncaminhamentoResponse.builder().tpLeito("81").build();
        when(service.buscarHospitais("3131307", "81", 2)).thenReturn(response);

        EncaminhamentoResponse result = useCase.executar("3131307", "dengue", "grave", "81", 2);

        assertThat(result).isEqualTo(response);
        verify(service).buscarHospitais("3131307", "81", 2);
    }

    @Test
    void executar_resolveTpLeitoQuandoNaoInformado() {
        EncaminhamentoService service = mock(EncaminhamentoService.class);
        ConsultarEncaminhamentoUseCase useCase = new ConsultarEncaminhamentoUseCase(service, new SimpleMeterRegistry());
        EncaminhamentoResponse response = EncaminhamentoResponse.builder().tpLeito("74").build();
        when(service.resolverTpLeito("moderada")).thenReturn("74");
        when(service.buscarHospitais("3131307", "74", 1)).thenReturn(response);

        EncaminhamentoResponse result = useCase.executar("3131307", "dengue", "moderada", null, 1);

        assertThat(result).isEqualTo(response);
        verify(service).resolverTpLeito("moderada");
    }
}
