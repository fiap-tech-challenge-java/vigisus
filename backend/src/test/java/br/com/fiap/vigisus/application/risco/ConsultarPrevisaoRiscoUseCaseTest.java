package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarPrevisaoRiscoUseCaseTest {

    @Test
    void buscarPorMunicipio_aplicaTextoIa() {
        PrevisaoRiscoService previsaoRiscoService = mock(PrevisaoRiscoService.class);
        IaService iaService = mock(IaService.class);
        ConsultarPrevisaoRiscoUseCase useCase = new ConsultarPrevisaoRiscoUseCase(previsaoRiscoService, iaService);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().municipio("Lavras").build();
        when(previsaoRiscoService.calcularRisco("3131307")).thenReturn(response);
        when(iaService.gerarTextoRisco(response)).thenReturn("texto risco");

        assertThat(useCase.buscarPorMunicipio("3131307").getTextoIa()).isEqualTo("texto risco");
    }
}
