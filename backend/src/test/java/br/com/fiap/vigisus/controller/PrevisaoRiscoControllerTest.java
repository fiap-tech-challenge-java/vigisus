package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import br.com.fiap.vigisus.service.RiscoAgregadoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrevisaoRiscoControllerTest {

    @Test
    void getPrevisaoRisco_aplicaTextoIa() {
        PrevisaoRiscoService previsaoService = mock(PrevisaoRiscoService.class);
        RiscoAgregadoService agregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoService, agregadoService, iaService);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().municipio("Lavras").build();
        when(previsaoService.calcularRisco("3131307")).thenReturn(response);
        when(iaService.gerarTextoRisco(response)).thenReturn("texto risco");

        assertThat(controller.getPrevisaoRisco("3131307", "dengue").getTextoIa()).isEqualTo("texto risco");
    }

    @Test
    void getRiscoBrasil_aplicaTextoIaQuandoHaResposta() {
        PrevisaoRiscoService previsaoService = mock(PrevisaoRiscoService.class);
        RiscoAgregadoService agregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoService, agregadoService, iaService);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().municipio("Brasil").build();
        when(agregadoService.calcularRiscoBrasil()).thenReturn(response);
        when(iaService.gerarTextoRisco(response)).thenReturn("texto brasil");

        assertThat(controller.getRiscoBrasil().getTextoIa()).isEqualTo("texto brasil");
    }

    @Test
    void getRiscoEstado_retornaNullQuandoServicoRetornaNull() {
        PrevisaoRiscoService previsaoService = mock(PrevisaoRiscoService.class);
        RiscoAgregadoService agregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoService, agregadoService, iaService);

        when(agregadoService.calcularRiscoEstado("XX")).thenReturn(null);

        assertThat(controller.getRiscoEstado("XX")).isNull();
    }

    @Test
    void getHospitaisBrasilEEstado_delegamAoService() {
        PrevisaoRiscoService previsaoService = mock(PrevisaoRiscoService.class);
        RiscoAgregadoService agregadoService = mock(RiscoAgregadoService.class);
        IaService iaService = mock(IaService.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoService, agregadoService, iaService);

        List<Estabelecimento> hospitaisBrasil = List.of(Estabelecimento.builder().coCnes("1").build());
        List<Estabelecimento> hospitaisEstado = List.of(Estabelecimento.builder().coCnes("2").build());
        when(agregadoService.buscarHospitaisBrasil()).thenReturn(hospitaisBrasil);
        when(agregadoService.buscarHospitaisEstado("MG")).thenReturn(hospitaisEstado);

        assertThat(controller.getHospitaisBrasil()).isEqualTo(hospitaisBrasil);
        assertThat(controller.getHospitaisEstado("MG")).isEqualTo(hospitaisEstado);
    }
}
