package br.com.fiap.vigisus.application.triagem;

import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.TriagemService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvaliarTriagemUseCaseTest {

    @Test
    void executar_delegaAoService() {
        TriagemService triagemService = mock(TriagemService.class);
        AvaliarTriagemUseCase useCase = new AvaliarTriagemUseCase(triagemService, new SimpleMeterRegistry());
        TriagemRequest request = TriagemRequest.builder()
                .municipio("3131307")
                .sintomas(List.of("febre"))
                .diasSintomas(2)
                .idade(20)
                .comorbidades(List.of())
                .build();
        TriagemResponse response = TriagemResponse.builder().prioridade("MEDIA").build();
        when(triagemService.avaliar(request)).thenReturn(response);

        assertThat(useCase.executar(request)).isEqualTo(response);
    }
}
