package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private MunicipioRepository municipioRepository;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    private RankingService service;

    private static final String UF = "MG";
    private static final int ANO = 2024;

    @BeforeEach
    void setUp() {
        service = new RankingService(municipioRepository, casoDengueRepository);
    }

    private Municipio municipio(String coIbge, String nome, long pop) {
        return Municipio.builder()
                .coIbge(coIbge)
                .noMunicipio(nome)
                .sgUf(UF)
                .populacao(pop)
                .build();
    }

    private static List<Object[]> casosRows(Object[]... rows) {
        return Arrays.asList(rows);
    }

    // ── calcularRanking: ordem piores (desc) ──────────────────────────────────

    @Test
    void calcularRanking_piores_ordenaDescendente() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                municipio("31002", "Beta", 100_000L),
                municipio("31003", "Gama", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        // Batch query: Alfa=100, Beta=300, Gama=200 casos
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(
                        new Object[]{"31001", 100L},
                        new Object[]{"31002", 300L},
                        new Object[]{"31003", 200L}
                ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "piores");

        assertThat(response.getUf()).isEqualTo(UF);
        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(3);
        assertThat(response.getRanking()).hasSize(3);
        // First = Beta (300 incidencia), Last = Alfa (100 incidencia)
        assertThat(response.getRanking().get(0).getCoIbge()).isEqualTo("31002");
        assertThat(response.getRanking().get(0).getPosicao()).isEqualTo(1);
        assertThat(response.getRanking().get(2).getCoIbge()).isEqualTo("31001");
        assertThat(response.getRanking().get(2).getPosicao()).isEqualTo(3);
    }

    // ── calcularRanking: ordem melhores (asc) ─────────────────────────────────

    @Test
    void calcularRanking_melhores_ordenaAscendente() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                municipio("31002", "Beta", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(
                        new Object[]{"31001", 100L},
                        new Object[]{"31002", 300L}
                ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "melhores");

        // First = Alfa (100 incidencia = lowest = best)
        assertThat(response.getRanking().get(0).getCoIbge()).isEqualTo("31001");
    }

    // ── calcularRanking: ignora município sem população ───────────────────────

    @Test
    void calcularRanking_ignoraMunicipioSemPopulacao() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                Municipio.builder().coIbge("31002").noMunicipio("SemPop").sgUf(UF).populacao(null).build(),
                Municipio.builder().coIbge("31003").noMunicipio("ZeroPop").sgUf(UF).populacao(0L).build()
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(new Object[]{"31001", 50L}));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "piores");

        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(1);
        assertThat(response.getRanking()).hasSize(1);
    }

    // ── calcularRanking: limita top N (máximo 100) ────────────────────────────

    @Test
    void calcularRanking_limitaTopN() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                municipio("31002", "Beta", 100_000L),
                municipio("31003", "Gama", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(
                        new Object[]{"31001", 100L},
                        new Object[]{"31002", 200L},
                        new Object[]{"31003", 300L}
                ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 2, "piores");

        assertThat(response.getRanking()).hasSize(2);
        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(3);
    }

    // ── calcularRanking: top acima de 100 fica limitado a 100 ────────────────

    @Test
    void calcularRanking_topAcimaDe100LimitaA100() {
        // 3 municipios, top=200 → devolvemos todos os 3
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                municipio("31002", "Beta", 100_000L),
                municipio("31003", "Gama", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(
                        new Object[]{"31001", 100L},
                        new Object[]{"31002", 200L},
                        new Object[]{"31003", 300L}
                ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 200, "piores");

        // Cannot exceed total municipalities even if top > 100
        assertThat(response.getRanking().size()).isLessThanOrEqualTo(100);
    }

    // ── calcularPosicaoNoEstado ───────────────────────────────────────────────

    @Test
    void calcularPosicaoNoEstado_retornaPosicaoCorreta() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L),
                municipio("31002", "Beta", 100_000L),
                municipio("31003", "Gama", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        // Beta=300 (pior), Gama=200, Alfa=100 (melhor)
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(
                        new Object[]{"31001", 100L},
                        new Object[]{"31002", 300L},
                        new Object[]{"31003", 200L}
                ));

        String posicaoAlfa = service.calcularPosicaoNoEstado("31001", UF, "dengue", ANO);
        String posicaoBeta = service.calcularPosicaoNoEstado("31002", UF, "dengue", ANO);
        String posicaoGama = service.calcularPosicaoNoEstado("31003", UF, "dengue", ANO);

        assertThat(posicaoBeta).isEqualTo("1 de 3");
        assertThat(posicaoGama).isEqualTo("2 de 3");
        assertThat(posicaoAlfa).isEqualTo("3 de 3");
    }

    @Test
    void calcularPosicaoNoEstado_retornaNullSeMunicipioNaoEncontrado() {
        List<Municipio> municipios = List.of(
                municipio("31001", "Alfa", 100_000L)
        );
        when(municipioRepository.findBySgUf(UF)).thenReturn(municipios);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioInAndAno(any(), anyInt()))
                .thenReturn(casosRows(new Object[]{"31001", 100L}));

        String posicao = service.calcularPosicaoNoEstado("99999", UF, "dengue", ANO);

        assertThat(posicao).isNull();
    }

    @Test
    void calcularPosicaoNoEstado_retornaNullSeUfSemMunicipios() {
        when(municipioRepository.findBySgUf(UF)).thenReturn(Collections.emptyList());

        String posicao = service.calcularPosicaoNoEstado("31001", UF, "dengue", ANO);

        assertThat(posicao).isNull();
    }
}
