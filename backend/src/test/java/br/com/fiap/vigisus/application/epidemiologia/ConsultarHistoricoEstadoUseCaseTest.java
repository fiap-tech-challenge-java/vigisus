package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.service.EstadoHistoricoService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHistoricoEstadoUseCaseTest {

    @Test
    void buscar_aplicaAnoPadrao() {
        EstadoHistoricoService estadoHistoricoService = mock(EstadoHistoricoService.class);
        ConsultarHistoricoEstadoUseCase useCase = new ConsultarHistoricoEstadoUseCase(estadoHistoricoService);
        PerfilEpidemiologicoResponse response = PerfilEpidemiologicoResponse.builder().uf("MG").build();

        when(estadoHistoricoService.gerarPerfilEstado("MG", "dengue", LocalDate.now().getYear()))
                .thenReturn(response);

        assertThat(useCase.buscar("MG", "dengue", null)).isEqualTo(response);
    }
}
