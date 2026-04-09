package br.com.fiap.vigisus.application.epidemiologia;

import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.RankingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarRankingMunicipalUseCaseTest {

    @Test
    void buscar_aplicaAnoPadrao() {
        RankingService rankingService = mock(RankingService.class);
        ConsultarRankingMunicipalUseCase useCase = new ConsultarRankingMunicipalUseCase(rankingService, new SimpleMeterRegistry());
        RankingResponse response = RankingResponse.builder().uf("MG").ranking(List.of()).build();

        when(rankingService.calcularRanking("MG", "dengue", LocalDate.now().getYear(), 20, "piores"))
                .thenReturn(response);

        assertThat(useCase.buscar("MG", "dengue", null, 20, "piores")).isEqualTo(response);
    }
}
