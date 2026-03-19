package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriagemServiceTest {

    @Mock
    private PerfilEpidemiologicoService perfilService;

    @Mock
    private EncaminhamentoService encaminhamentoService;

    @Mock
    private IaService iaService;

    private TriagemService service;

    private static final String CO_IBGE = "3131307";

    @BeforeEach
    void setUp() {
        service = new TriagemService(perfilService, encaminhamentoService, iaService);
    }

    // ── testPacienteComSinaisAlarmaDeveSerCritico ─────────────────────────────

    @Test
    void testPacienteComSinaisAlarmaDeveSerCritico() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .coIbge(CO_IBGE)
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(500L)
                .incidencia(350.0)
                .classificacao("EPIDEMIA")
                .build();

        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenReturn(perfil);

        EncaminhamentoResponse encResp = EncaminhamentoResponse.builder()
                .coIbge(CO_IBGE)
                .municipioOrigem("Lavras")
                .tpLeito("81")
                .hospitais(List.of(
                        EncaminhamentoResponse.HospitalDTO.builder()
                                .coCnes("CNES001")
                                .noFantasia("Hospital Central")
                                .distanciaKm(5.0)
                                .build()))
                .build();
        when(encaminhamentoService.buscarHospitais(anyString(), anyString(), anyInt())).thenReturn(encResp);
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientação de triagem.");

        TriagemRequest req = TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre", "dor_abdominal", "sangramento", "vomito"))
                .comorbidades(List.of("diabetes"))
                .diasSintomas(3)
                .idade(40)
                .build();

        TriagemResponse resp = service.avaliar(req);

        assertThat(resp.getPrioridade()).isEqualTo("CRITICA");
        assertThat(resp.getCorProtocolo()).isEqualTo("VERMELHO");
        assertThat(resp.getEncaminhamento()).isNotNull();
        assertThat(resp.isRequerObservacao()).isTrue();
    }

    // ── testPacienteJovemSintomesClassicosDevSerMedia ─────────────────────────

    @Test
    void testPacienteJovemSintomesClassicosDevSerMedia() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .coIbge(CO_IBGE)
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(80L)
                .incidencia(80.0)
                .classificacao("MODERADO")
                .build();

        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenReturn(perfil);
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientação.");

        TriagemRequest req = TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre", "dor_muscular", "cefaleia"))
                .comorbidades(List.of())
                .diasSintomas(2)
                .idade(25)
                .build();

        TriagemResponse resp = service.avaliar(req);

        // Score: febre(1) + dor_muscular(1) + cefaleia(1) = 3, × 1.0 (MODERADO) = 3.0 → MEDIA
        assertThat(resp.getPrioridade()).isEqualTo("MEDIA");
        assertThat(resp.getCorProtocolo()).isEqualTo("AMARELO");
    }

    // ── testContextoEpidemiaAumentaScore ─────────────────────────────────────

    @Test
    void testContextoEpidemiaAumentaScore() {
        PerfilEpidemiologicoResponse perfilEpidemia = PerfilEpidemiologicoResponse.builder()
                .coIbge(CO_IBGE)
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(500L)
                .incidencia(400.0)
                .classificacao("EPIDEMIA")
                .build();

        PerfilEpidemiologicoResponse perfilBaixo = PerfilEpidemiologicoResponse.builder()
                .coIbge("9999999")
                .municipio("Outra Cidade")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(20L)
                .incidencia(20.0)
                .classificacao("BAIXO")
                .build();

        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenReturn(perfilEpidemia);
        when(perfilService.gerarPerfil(eq("9999999"), eq("dengue"), anyInt())).thenReturn(perfilBaixo);
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientação.");

        TriagemRequest reqEpidemia = TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre", "cefaleia"))
                .comorbidades(List.of())
                .diasSintomas(2)
                .idade(30)
                .build();

        TriagemRequest reqBaixo = TriagemRequest.builder()
                .municipio("9999999")
                .sintomas(List.of("febre", "cefaleia"))
                .comorbidades(List.of())
                .diasSintomas(2)
                .idade(30)
                .build();

        // Same symptoms — score in epidemic context should be higher
        double scoreEpidemia = service.calcularScore(reqEpidemia) * service.resolverMultiplicador("EPIDEMIA");
        double scoreBaixo = service.calcularScore(reqBaixo) * service.resolverMultiplicador("BAIXO");

        assertThat(scoreEpidemia).isGreaterThan(scoreBaixo);

        // Full evaluation: epidemic → higher priority than low context
        TriagemResponse respEpidemia = service.avaliar(reqEpidemia);
        TriagemResponse respBaixo = service.avaliar(reqBaixo);

        // Both could be MEDIA, but epidemic context should produce equal or higher priority
        assertThat(priorityRank(respEpidemia.getPrioridade()))
                .isGreaterThanOrEqualTo(priorityRank(respBaixo.getPrioridade()));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private int priorityRank(String prioridade) {
        return switch (prioridade) {
            case "CRITICA" -> 4;
            case "ALTA"    -> 3;
            case "MEDIA"   -> 2;
            default        -> 1;
        };
    }
}
