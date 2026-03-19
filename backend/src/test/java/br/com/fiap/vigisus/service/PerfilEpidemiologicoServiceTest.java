package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilEpidemiologicoServiceTest {

    @Mock
    private MunicipioService municipioService;

    @Mock
    private CasoDengueRepository casoDengueRepository;

    private PerfilEpidemiologicoService service;

    private static final String CO_IBGE = "3131307";
    private static final long POPULACAO = 102_000L;

    @BeforeEach
    void setUp() {
        service = new PerfilEpidemiologicoService(municipioService, casoDengueRepository);
    }

    // ── testCalculaIncidenciaCorretamente ─────────────────────────────────────

    @Test
    void testCalculaIncidenciaCorretamente() {
        // 306 cases / 102 000 pop * 100 000 = 300 per 100k → ALTO
        Municipio municipio = Municipio.builder()
                .coIbge(CO_IBGE)
                .noMunicipio("Lavras")
                .sgUf("MG")
                .populacao(POPULACAO)
                .build();
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipio);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024))
                .thenReturn(306L);

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        double expectedIncidencia = 306.0 / POPULACAO * 100_000;
        assertThat(response.getIncidencia()).isCloseTo(expectedIncidencia, within(0.01));
        assertThat(response.getTotal()).isEqualTo(306L);
    }

    // ── testClassificaEpidemiaAcimaDe300por100k ───────────────────────────────

    @Test
    void testClassificaEpidemiaAcimaDe300por100k() {
        // 307 cases / 102 000 pop * 100 000 ≈ 300.98 per 100k → EPIDEMIA (> 300)
        Municipio municipio = Municipio.builder()
                .coIbge(CO_IBGE)
                .noMunicipio("Lavras")
                .sgUf("MG")
                .populacao(POPULACAO)
                .build();
        when(municipioService.buscarPorCoIbge(CO_IBGE)).thenReturn(municipio);
        when(casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(CO_IBGE, 2024))
                .thenReturn(307L);

        PerfilEpidemiologicoResponse response = service.gerarPerfil(CO_IBGE, "dengue", 2024);

        assertThat(response.getIncidencia()).isGreaterThan(300.0);
        assertThat(response.getClassificacao()).isEqualTo("EPIDEMIA");
    }

    // ── classificar thresholds (boundary tests) ────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "0.0,    BAIXO",
            "49.99,  BAIXO",
            "50.0,   MODERADO",
            "99.99,  MODERADO",
            "100.0,  ALTO",
            "300.0,  ALTO",
            "300.01, EPIDEMIA",
            "500.0,  EPIDEMIA"
    })
    void classificar_thresholds(double incidencia, String expected) {
        String result = classificar(incidencia);
        assertThat(result).isEqualTo(expected);
    }

    private String classificar(double incidencia) {
        if (incidencia < 50) return "BAIXO";
        if (incidencia < 100) return "MODERADO";
        if (incidencia <= 300) return "ALTO";
        return "EPIDEMIA";
    }
}
