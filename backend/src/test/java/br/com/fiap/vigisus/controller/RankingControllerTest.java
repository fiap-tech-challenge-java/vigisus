package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarHistoricoEstadoUseCase;
import br.com.fiap.vigisus.application.epidemiologia.ConsultarRankingMunicipalUseCase;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.RankingResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingControllerTest {

    @Test
    void getRanking_delegaParaUseCase() {
        ConsultarRankingMunicipalUseCase rankingUseCase = mock(ConsultarRankingMunicipalUseCase.class);
        ConsultarHistoricoEstadoUseCase historicoUseCase = mock(ConsultarHistoricoEstadoUseCase.class);
        RankingController controller = new RankingController(rankingUseCase, historicoUseCase);
        RankingResponse response = RankingResponse.builder().uf("MG").ranking(List.of()).build();

        when(rankingUseCase.buscar("MG", "dengue", null, 20, "piores")).thenReturn(response);

        assertThat(controller.getRanking("MG", "dengue", null, 20, "piores")).isEqualTo(response);
    }

    @Test
    void getHistoricoEstado_delegaParaUseCase() {
        ConsultarRankingMunicipalUseCase rankingUseCase = mock(ConsultarRankingMunicipalUseCase.class);
        ConsultarHistoricoEstadoUseCase historicoUseCase = mock(ConsultarHistoricoEstadoUseCase.class);
        RankingController controller = new RankingController(rankingUseCase, historicoUseCase);
        PerfilEpidemiologicoResponse response = PerfilEpidemiologicoResponse.builder().uf("MG").build();

        when(historicoUseCase.buscar("MG", "dengue", null)).thenReturn(response);

        assertThat(controller.getHistoricoEstado("MG", "dengue", null)).isEqualTo(response);
    }
}
