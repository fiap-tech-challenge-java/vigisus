package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.risco.ConsultarPrevisaoRiscoUseCase;
import br.com.fiap.vigisus.application.risco.ConsultarRiscoAgregadoUseCase;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Estabelecimento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrevisaoRiscoControllerTest {

    @Test
    void getPrevisaoRisco_delegaParaUseCase() {
        ConsultarPrevisaoRiscoUseCase previsaoUseCase = mock(ConsultarPrevisaoRiscoUseCase.class);
        ConsultarRiscoAgregadoUseCase agregadoUseCase = mock(ConsultarRiscoAgregadoUseCase.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoUseCase, agregadoUseCase);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().textoIa("texto risco").build();
        when(previsaoUseCase.buscarPorMunicipio("3131307")).thenReturn(response);

        assertThat(controller.getPrevisaoRisco("3131307", "dengue").getTextoIa()).isEqualTo("texto risco");
    }

    @Test
    void getRiscoBrasil_delegaParaUseCase() {
        ConsultarPrevisaoRiscoUseCase previsaoUseCase = mock(ConsultarPrevisaoRiscoUseCase.class);
        ConsultarRiscoAgregadoUseCase agregadoUseCase = mock(ConsultarRiscoAgregadoUseCase.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoUseCase, agregadoUseCase);

        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().textoIa("texto brasil").build();
        when(agregadoUseCase.buscarBrasil()).thenReturn(response);

        assertThat(controller.getRiscoBrasil().getTextoIa()).isEqualTo("texto brasil");
    }

    @Test
    void getRiscoEstado_retornaNullQuandoUseCaseRetornaNull() {
        ConsultarPrevisaoRiscoUseCase previsaoUseCase = mock(ConsultarPrevisaoRiscoUseCase.class);
        ConsultarRiscoAgregadoUseCase agregadoUseCase = mock(ConsultarRiscoAgregadoUseCase.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoUseCase, agregadoUseCase);

        when(agregadoUseCase.buscarEstado("XX")).thenReturn(null);

        assertThat(controller.getRiscoEstado("XX")).isNull();
    }

    @Test
    void getHospitaisBrasilEEstado_delegamAoUseCase() {
        ConsultarPrevisaoRiscoUseCase previsaoUseCase = mock(ConsultarPrevisaoRiscoUseCase.class);
        ConsultarRiscoAgregadoUseCase agregadoUseCase = mock(ConsultarRiscoAgregadoUseCase.class);
        PrevisaoRiscoController controller = new PrevisaoRiscoController(previsaoUseCase, agregadoUseCase);

        List<Estabelecimento> hospitaisBrasil = List.of(Estabelecimento.builder().coCnes("1").build());
        List<Estabelecimento> hospitaisEstado = List.of(Estabelecimento.builder().coCnes("2").build());
        when(agregadoUseCase.buscarHospitaisBrasil()).thenReturn(hospitaisBrasil);
        when(agregadoUseCase.buscarHospitaisEstado("MG")).thenReturn(hospitaisEstado);

        assertThat(controller.getHospitaisBrasil()).isEqualTo(hospitaisBrasil);
        assertThat(controller.getHospitaisEstado("MG")).isEqualTo(hospitaisEstado);
    }
}
