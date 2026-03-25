package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.RankingResponse;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static List<Object[]> rankingRows(Object[]... rows) {
        return Arrays.asList(rows);
    }

    @Test
    void calcularRanking_piores_ordenaDescendente() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rankingRows(
                new Object[]{"31001", "Alfa", UF, 100L, 100_000L},
                new Object[]{"31002", "Beta", UF, 300L, 100_000L},
                new Object[]{"31003", "Gama", UF, 200L, 100_000L}
        ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "piores");

        assertThat(response.getUf()).isEqualTo(UF);
        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(3);
        assertThat(response.getRanking()).hasSize(3);
        assertThat(response.getRanking().get(0).getCoIbge()).isEqualTo("31002");
        assertThat(response.getRanking().get(0).getPosicao()).isEqualTo(1);
        assertThat(response.getRanking().get(2).getCoIbge()).isEqualTo("31001");
        assertThat(response.getRanking().get(2).getPosicao()).isEqualTo(3);
    }

    @Test
    void calcularRanking_melhores_ordenaAscendente() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rankingRows(
                new Object[]{"31001", "Alfa", UF, 100L, 100_000L},
                new Object[]{"31002", "Beta", UF, 300L, 100_000L}
        ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "melhores");

        assertThat(response.getRanking().get(0).getCoIbge()).isEqualTo("31001");
        assertThat(response.getRanking().get(1).getCoIbge()).isEqualTo("31002");
    }

    @Test
    void calcularRanking_ignoraMunicipioSemPopulacao() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rankingRows(
                new Object[]{"31001", "Alfa", UF, 50L, 100_000L},
                new Object[]{"31002", "SemPop", UF, 20L, null},
                new Object[]{"31003", "ZeroPop", UF, 10L, 0L}
        ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 20, "piores");

        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(1);
        assertThat(response.getRanking()).hasSize(1);
        assertThat(response.getRanking().get(0).getCoIbge()).isEqualTo("31001");
    }

    @Test
    void calcularRanking_limitaTopN() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rankingRows(
                new Object[]{"31001", "Alfa", UF, 100L, 100_000L},
                new Object[]{"31002", "Beta", UF, 200L, 100_000L},
                new Object[]{"31003", "Gama", UF, 300L, 100_000L}
        ));

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 2, "piores");

        assertThat(response.getRanking()).hasSize(2);
        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(3);
    }

    @Test
    void calcularRanking_topAcimaDoLimiteInterno() {
        List<Object[]> rows = IntStream.rangeClosed(1, 1005)
                .mapToObj(i -> new Object[]{
                        String.format("%05d", i),
                        "Municipio " + i,
                        UF,
                        (long) i,
                        100_000L
                })
                .toList();
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rows);

        RankingResponse response = service.calcularRanking(UF, "dengue", ANO, 1500, "piores");

        assertThat(response.getTotalMunicipiosComDados()).isEqualTo(1005);
        assertThat(response.getRanking()).hasSize(1000);
    }

    @Test
    void calcularPosicaoNoEstado_retornaPosicaoCorreta() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(rankingRows(
                new Object[]{"31001", "Alfa", UF, 100L, 100_000L},
                new Object[]{"31002", "Beta", UF, 300L, 100_000L},
                new Object[]{"31003", "Gama", UF, 200L, 100_000L}
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
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(
                rankingRows(new Object[]{"31001", "Alfa", UF, 100L, 100_000L}));

        String posicao = service.calcularPosicaoNoEstado("99999", UF, "dengue", ANO);

        assertThat(posicao).isNull();
    }

    @Test
    void calcularPosicaoNoEstado_retornaNullSeUfSemMunicipios() {
        when(casoDengueRepository.rankingOtimizadoPorEstado(UF, ANO)).thenReturn(Collections.emptyList());

        String posicao = service.calcularPosicaoNoEstado("31001", UF, "dengue", ANO);

        assertThat(posicao).isNull();
    }
}
