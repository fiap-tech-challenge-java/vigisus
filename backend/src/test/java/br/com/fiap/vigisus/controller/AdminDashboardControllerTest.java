package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.epidemiologia.ConsultarBrasilEpidemiologicoUseCase;
import br.com.fiap.vigisus.application.epidemiologia.ConsultarRankingMunicipalUseCase;
import br.com.fiap.vigisus.dto.AdminBuscaIaDTO;
import br.com.fiap.vigisus.dto.AdminResumoDTO;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.EstadoDTO;
import br.com.fiap.vigisus.dto.MunicipioRiscoDTO;
import br.com.fiap.vigisus.dto.RankingMunicipioDTO;
import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.service.IaBuscaTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardControllerTest {

    private ConsultarBrasilEpidemiologicoUseCase brasilUseCase;
    private ConsultarRankingMunicipalUseCase rankingUseCase;
    private IaBuscaTracker tracker;
    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        brasilUseCase = mock(ConsultarBrasilEpidemiologicoUseCase.class);
        rankingUseCase = mock(ConsultarRankingMunicipalUseCase.class);
        tracker = mock(IaBuscaTracker.class);
        controller = new AdminDashboardController(brasilUseCase, rankingUseCase, tracker);
    }

    @Test
    void getResumo_retornaKpisNacionais() {
        EstadoDTO est1 = EstadoDTO.builder().sgUf("SP").totalCasos(100_000L).build();
        EstadoDTO est2 = EstadoDTO.builder().sgUf("MG").totalCasos(50_000L).build();
        MunicipioRiscoDTO mun1 = MunicipioRiscoDTO.builder().municipio("Campinas").classificacao("EPIDEMIA").build();
        MunicipioRiscoDTO mun2 = MunicipioRiscoDTO.builder().municipio("BH").classificacao("ALTO").build();

        BrasilEpidemiologicoResponse resp = BrasilEpidemiologicoResponse.builder()
                .totalCasos(500_000L)
                .incidencia(234.5)
                .classificacao("EPIDEMIA")
                .tendencia("CRESCENTE")
                .doenca("dengue")
                .ano(2025)
                .estadosPiores(List.of(est1, est2))
                .municipiosPiores(List.of(mun1, mun2))
                .build();

        when(brasilUseCase.buscar(eq("dengue"), anyInt())).thenReturn(resp);

        AdminResumoDTO resumo = controller.getResumo("dengue", null);

        assertThat(resumo.getTotalCasos()).isEqualTo(500_000L);
        assertThat(resumo.getClassificacaoNacional()).isEqualTo("EPIDEMIA");
        assertThat(resumo.getTotalEstadosAfetados()).isEqualTo(2);
        assertThat(resumo.getMunicipiosEpidemia()).isEqualTo(1);
        assertThat(resumo.getMunicipiosAltoRisco()).isEqualTo(1);
    }

    @Test
    void getTopMunicipios_retornaListaDoRanking() {
        RankingMunicipioDTO mun = RankingMunicipioDTO.builder()
                .posicao(1).municipio("Campinas").incidencia100k(1500.0).build();
        RankingResponse rankResp = RankingResponse.builder().ranking(List.of(mun)).build();

        when(rankingUseCase.buscar(any(), anyString(), any(), eq(10), eq("piores"))).thenReturn(rankResp);

        List<RankingMunicipioDTO> result = controller.getTopMunicipios(10, "dengue", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMunicipio()).isEqualTo("Campinas");
    }

    @Test
    void getTopEstados_retornaEstadosPiores() {
        EstadoDTO e = EstadoDTO.builder().sgUf("SP").totalCasos(99_999L).build();
        BrasilEpidemiologicoResponse resp = BrasilEpidemiologicoResponse.builder()
                .estadosPiores(List.of(e)).doenca("dengue").ano(2025).build();

        when(brasilUseCase.buscar(anyString(), anyInt())).thenReturn(resp);

        List<EstadoDTO> estados = controller.getTopEstados("dengue", null);

        assertThat(estados).hasSize(1);
        assertThat(estados.get(0).getSgUf()).isEqualTo("SP");
    }

    @Test
    void getMunicipiosRisco_retornaMunicipiosPiores() {
        MunicipioRiscoDTO m = MunicipioRiscoDTO.builder().municipio("BH").classificacao("ALTO").build();
        BrasilEpidemiologicoResponse resp = BrasilEpidemiologicoResponse.builder()
                .municipiosPiores(List.of(m)).doenca("dengue").ano(2025).build();

        when(brasilUseCase.buscar(anyString(), anyInt())).thenReturn(resp);

        List<MunicipioRiscoDTO> risco = controller.getMunicipiosRisco("dengue", null);

        assertThat(risco).hasSize(1);
        assertThat(risco.get(0).getClassificacao()).isEqualTo("ALTO");
    }

    @Test
    void getBuscasIa_delegaParaTracker() {
        AdminBuscaIaDTO q = AdminBuscaIaDTO.builder().pergunta("dengue em SP").contagem(5L).build();
        when(tracker.listarMaisFrequentes(20)).thenReturn(List.of(q));

        List<AdminBuscaIaDTO> result = controller.getBuscasIa(20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPergunta()).isEqualTo("dengue em SP");
    }

    @Test
    void getResumo_retornaZerosQuandoListasNulas() {
        BrasilEpidemiologicoResponse resp = BrasilEpidemiologicoResponse.builder()
                .totalCasos(0L).doenca("dengue").ano(2025).build();

        when(brasilUseCase.buscar(anyString(), anyInt())).thenReturn(resp);

        AdminResumoDTO resumo = controller.getResumo("dengue", null);

        assertThat(resumo.getTotalEstadosAfetados()).isZero();
        assertThat(resumo.getMunicipiosEpidemia()).isZero();
    }
}
