package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.RiscoAgregadoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarRiscoAgregadoUseCaseTest {

    @Test
    void buscarBrasil_aplicaTextoIaQuandoHaResposta() {
        RiscoAgregadoService riscoAgregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        ConsultarRiscoAgregadoUseCase useCase = new ConsultarRiscoAgregadoUseCase(riscoAgregadoService, iaService);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().municipio("Brasil").build();
        when(riscoAgregadoService.calcularRiscoBrasil()).thenReturn(response);
        when(iaService.gerarTextoRisco(response)).thenReturn("texto brasil");

        assertThat(useCase.buscarBrasil().getTextoIa()).isEqualTo("texto brasil");
    }

    @Test
    void buscarEstado_retornaNullQuandoServicoRetornaNull() {
        RiscoAgregadoService riscoAgregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        ConsultarRiscoAgregadoUseCase useCase = new ConsultarRiscoAgregadoUseCase(riscoAgregadoService, iaService);

        when(riscoAgregadoService.calcularRiscoEstado("XX")).thenReturn(null);

        assertThat(useCase.buscarEstado("XX")).isNull();
    }

    @Test
    void buscarHospitais_delegaAoService() {
        RiscoAgregadoService riscoAgregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        ConsultarRiscoAgregadoUseCase useCase = new ConsultarRiscoAgregadoUseCase(riscoAgregadoService, iaService);

        List<Estabelecimento> hospitaisBrasil = List.of(Estabelecimento.builder().coCnes("1").build());
        List<Estabelecimento> hospitaisEstado = List.of(Estabelecimento.builder().coCnes("2").build());
        when(riscoAgregadoService.buscarHospitaisBrasil()).thenReturn(hospitaisBrasil);
        when(riscoAgregadoService.buscarHospitaisEstado("MG")).thenReturn(hospitaisEstado);

        assertThat(useCase.buscarHospitaisBrasil()).isEqualTo(hospitaisBrasil);
        assertThat(useCase.buscarHospitaisEstado("MG")).isEqualTo(hospitaisEstado);
    }
}
