package br.com.fiap.vigisus.application.risco;

import br.com.fiap.vigisus.domain.risco.RiscoAltoDetectadoEvent;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarPrevisaoRiscoUseCaseTest {

    private final PrevisaoRiscoService previsaoRiscoService = mock(PrevisaoRiscoService.class);
    private final IaService iaService = mock(IaService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ConsultarPrevisaoRiscoUseCase useCase =
        new ConsultarPrevisaoRiscoUseCase(previsaoRiscoService, iaService, eventPublisher);

    @Test
    void buscarPorMunicipio_aplicaTextoIa() {
        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder().municipio("Lavras").build();
        when(previsaoRiscoService.calcularRisco("3131307")).thenReturn(response);
        when(iaService.gerarTextoRisco(response)).thenReturn("texto risco");

        assertThat(useCase.buscarPorMunicipio("3131307").getTextoIa()).isEqualTo("texto risco");
    }

    @Test
    void buscarPorMunicipio_publicaEventoQuandoClassificacaoAlto() {
        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder()
            .coIbge("3550308").municipio("Campinas").uf("SP")
            .classificacao("ALTO").score(5)
            .build();
        when(previsaoRiscoService.calcularRisco("3550308")).thenReturn(response);

        useCase.buscarPorMunicipio("3550308");

        verify(eventPublisher).publishEvent(any(RiscoAltoDetectadoEvent.class));
    }

    @Test
    void buscarPorMunicipio_publicaEventoQuandoClassificacaoMuitoAlto() {
        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder()
            .coIbge("3550308").municipio("Campinas").uf("SP")
            .classificacao("MUITO_ALTO").score(7)
            .build();
        when(previsaoRiscoService.calcularRisco("3550308")).thenReturn(response);

        useCase.buscarPorMunicipio("3550308");

        verify(eventPublisher).publishEvent(any(RiscoAltoDetectadoEvent.class));
    }

    @Test
    void buscarPorMunicipio_naoPublicaEventoQuandoClassificacaoBaixa() {
        PrevisaoRiscoResponse response = PrevisaoRiscoResponse.builder()
            .coIbge("3131307").municipio("Lavras").uf("MG")
            .classificacao("BAIXO").score(1)
            .build();
        when(previsaoRiscoService.calcularRisco("3131307")).thenReturn(response);

        useCase.buscarPorMunicipio("3131307");

        verify(eventPublisher, never()).publishEvent(any());
    }
}
