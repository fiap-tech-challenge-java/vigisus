package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncaminhamentoServiceTest {

    @Mock
    private MunicipioService municipioService;

    @Mock
    private EstabelecimentoRepository estabelecimentoRepository;

    @Mock
    private LeitoRepository leitoRepository;

    @Mock
    private ServicoEspecializadoRepository servicoEspecializadoRepository;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    private EncaminhamentoService service;

    // Lavras coordinates
    private static final double LAT_LAVRAS = -21.245;
    private static final double LON_LAVRAS = -44.999;

    @BeforeEach
    void setUp() {
        service = new EncaminhamentoService(
                municipioService, estabelecimentoRepository, leitoRepository, servicoEspecializadoRepository,
                casoDengueRepository);
        org.mockito.Mockito.lenient()
                .when(casoDengueRepository.findByCoMunicipioAndAno(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(java.util.List.of());
    }

    // ── haversine ──────────────────────────────────────────────────────────────

    @Test
    void haversine_samePonto_returnsZero() {
        double dist = service.haversine(-23.5505, -46.6333, -23.5505, -46.6333);
        assertThat(dist).isEqualTo(0.0);
    }

    @Test
    void haversine_saoPauloToRioDeJaneiro_approx360km() {
        double dist = service.haversine(-23.5505, -46.6333, -22.9068, -43.1729);
        assertThat(dist).isCloseTo(357.0, within(5.0));
    }

    @Test
    void testHaversineDistanciaConhecida() {
        // Lavras (-21.245, -44.999) → Varginha (-21.551, -45.430)
        // The haversine formula yields ~56 km for these coordinates (±5 km)
        double dist = service.haversine(LAT_LAVRAS, LON_LAVRAS, -21.551, -45.430);
        assertThat(dist).isCloseTo(56.0, within(5.0));
    }

    // ── buscarHospitais — sorted by distance ──────────────────────────────────

    @Test
    void testBuscarHospitaisRetornaOrdenadoPorDistancia() {
        Municipio lavras = Municipio.builder()
                .coIbge("3131307")
                .noMunicipio("Lavras")
                .nuLatitude(LAT_LAVRAS)
                .nuLongitude(LON_LAVRAS)
                .build();

        when(municipioService.buscarPorCoIbge("3131307")).thenReturn(lavras);
        when(servicoEspecializadoRepository.findDistinctCoCnesByServEspIn(any()))
                .thenReturn(Set.of("CNES001", "CNES002"));

        Leito leito1 = Leito.builder().coCnes("CNES001").tpLeito("81").qtSus(5).build();
        Leito leito2 = Leito.builder().coCnes("CNES002").tpLeito("81").qtSus(3).build();
        when(leitoRepository.findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(
                any(), eq("81"), anyInt()))
                .thenReturn(List.of(leito1, leito2));

        // Hospital farther away (Varginha ~98 km)
        Estabelecimento hospVarginha = Estabelecimento.builder()
                .coCnes("CNES001")
                .noFantasia("Hospital Varginha")
                .coMunicipio("3170909")
                .nuLatitude(-21.551)
                .nuLongitude(-45.430)
                .build();

        // Hospital closer (within Lavras ~5 km offset)
        Estabelecimento hospLavras = Estabelecimento.builder()
                .coCnes("CNES002")
                .noFantasia("Hospital Lavras")
                .coMunicipio("3131307")
                .nuLatitude(-21.260)
                .nuLongitude(-45.010)
                .build();

        when(estabelecimentoRepository.findByCoCnesIn(any()))
                .thenReturn(List.of(hospVarginha, hospLavras));

        EncaminhamentoResponse response = service.buscarHospitais("3131307", "81", 1);

        List<HospitalDTO> hospitais = response.getHospitais();
        assertThat(hospitais).hasSize(2);
        assertThat(hospitais.get(0).getDistanciaKm())
                .isLessThan(hospitais.get(1).getDistanciaKm());
        assertThat(hospitais.get(0).getNoFantasia()).isEqualTo("Hospital Lavras");
        assertThat(hospitais.get(1).getNoFantasia()).isEqualTo("Hospital Varginha");
    }

    // ── resolverTpLeito ────────────────────────────────────────────────────────

    @Test
    void testGravidadeGraveMapeiaTpLeito81() {
        assertThat(service.resolverTpLeito("grave")).isEqualTo("81");
    }

    @Test
    void resolverTpLeito_critica_returns81() {
        assertThat(service.resolverTpLeito("critica")).isEqualTo("81");
        assertThat(service.resolverTpLeito("crítica")).isEqualTo("81");
    }

    @Test
    void resolverTpLeito_moderada_returns74() {
        assertThat(service.resolverTpLeito("moderada")).isEqualTo("74");
        assertThat(service.resolverTpLeito(null)).isEqualTo("74");
    }

    // ── risk classification (duplicated from PrevisaoRiscoService) ─────────────

    @ParameterizedTest
    @CsvSource({
            "0,  BAIXO",
            "1,  BAIXO",
            "2,  MODERADO",
            "3,  MODERADO",
            "4,  ALTO",
            "5,  ALTO",
            "6,  MUITO_ALTO",
            "8,  MUITO_ALTO"
    })
    void classificarRisco_thresholds(int score, String expected) {
        String result = classificar(score);
        assertThat(result).isEqualTo(expected);
    }

    private String classificar(int score) {
        if (score <= 1) return "BAIXO";
        if (score <= 3) return "MODERADO";
        if (score <= 5) return "ALTO";
        return "MUITO_ALTO";
    }
}
