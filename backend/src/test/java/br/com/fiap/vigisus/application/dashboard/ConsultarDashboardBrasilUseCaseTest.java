package br.com.fiap.vigisus.application.dashboard;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.application.risco.ConsultarRiscoAgregadoUseCase;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.DashboardBrasilResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarDashboardBrasilUseCaseTest {

    @Test
    void buscar_agregaPerfilRiscoEHospitaisEmUmaResposta() {
        ConsultarBrasilEpidemiologicoUseCase brasilUseCase = mock(ConsultarBrasilEpidemiologicoUseCase.class);
        ConsultarRiscoAgregadoUseCase riscoUseCase = mock(ConsultarRiscoAgregadoUseCase.class);
        ConsultarDashboardBrasilUseCase useCase = new ConsultarDashboardBrasilUseCase(brasilUseCase, riscoUseCase);

        BrasilEpidemiologicoResponse brasil = BrasilEpidemiologicoResponse.builder()
                .totalCasos(100L)
                .build();
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder()
                .classificacao("ALTO")
                .build();
        List<Estabelecimento> hospitais = List.of(Estabelecimento.builder().coCnes("123").build());

        when(brasilUseCase.buscar("dengue", 2025)).thenReturn(brasil);
        when(riscoUseCase.buscarBrasil()).thenReturn(risco);
        when(riscoUseCase.buscarHospitaisBrasil()).thenReturn(hospitais);

        DashboardBrasilResponse response = useCase.buscar("dengue", 2025);

        assertThat(response.getBrasil()).isSameAs(brasil);
        assertThat(response.getRisco()).isSameAs(risco);
        assertThat(response.getHospitais()).containsExactlyElementsOf(hospitais);
    }
}
