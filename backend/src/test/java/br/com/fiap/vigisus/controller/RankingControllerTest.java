package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.EstadoHistoricoService;
import br.com.fiap.vigisus.service.RankingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RankingControllerTest {

    @Test
    void getRanking_aplicaAnoPadrao() {
        RankingService rankingService = mock(RankingService.class);
        EstadoHistoricoService estadoHistoricoService = mock(EstadoHistoricoService.class);
        RankingController controller = new RankingController(rankingService, estadoHistoricoService);
        RankingResponse response = RankingResponse.builder().uf("MG").ranking(List.of()).build();
        when(rankingService.calcularRanking("MG", "dengue", LocalDate.now().getYear(), 20, "piores"))
                .thenReturn(response);

        assertThat(controller.getRanking("MG", "dengue", null, 20, "piores")).isEqualTo(response);
    }

    @Test
    void getHistoricoEstado_aplicaAnoPadrao() {
        RankingService rankingService = mock(RankingService.class);
        EstadoHistoricoService estadoHistoricoService = mock(EstadoHistoricoService.class);
        RankingController controller = new RankingController(rankingService, estadoHistoricoService);
        PerfilEpidemiologicoResponse response = PerfilEpidemiologicoResponse.builder().uf("MG").build();
        when(estadoHistoricoService.gerarPerfilEstado("MG", "dengue", LocalDate.now().getYear())).thenReturn(response);

        assertThat(controller.getHistoricoEstado("MG", "dengue", null)).isEqualTo(response);
    }
}
