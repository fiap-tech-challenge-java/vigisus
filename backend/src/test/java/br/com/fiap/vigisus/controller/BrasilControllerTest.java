package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.dashboard.ConsultarDashboardBrasilUseCase;
import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.DashboardBrasilResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrasilControllerTest {

    @Test
    void getCasosBrasil_delegaParaUseCase() {
        ConsultarBrasilEpidemiologicoUseCase useCase = mock(ConsultarBrasilEpidemiologicoUseCase.class);
        ConsultarDashboardBrasilUseCase dashboardUseCase = mock(ConsultarDashboardBrasilUseCase.class);
        BrasilController controller = new BrasilController(useCase, dashboardUseCase);
        BrasilEpidemiologicoResponse perfil = BrasilEpidemiologicoResponse.builder().textoIa("texto").build();

        when(useCase.buscar("dengue", null)).thenReturn(perfil);

        BrasilEpidemiologicoResponse response = controller.getCasosBrasil("dengue", null);

        assertThat(response.getTextoIa()).isEqualTo("texto");
    }

    @Test
    void getDashboardBrasil_delegaParaUseCase() {
        ConsultarBrasilEpidemiologicoUseCase useCase = mock(ConsultarBrasilEpidemiologicoUseCase.class);
        ConsultarDashboardBrasilUseCase dashboardUseCase = mock(ConsultarDashboardBrasilUseCase.class);
        BrasilController controller = new BrasilController(useCase, dashboardUseCase);
        DashboardBrasilResponse dashboard = DashboardBrasilResponse.builder().build();

        when(dashboardUseCase.buscar("dengue", 2025)).thenReturn(dashboard);

        DashboardBrasilResponse response = controller.getDashboardBrasil("dengue", 2025);

        assertThat(response).isSameAs(dashboard);
    }
}
