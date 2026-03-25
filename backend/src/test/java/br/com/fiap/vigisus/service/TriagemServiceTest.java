package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.domain.triagem.CalculadoraScoreTriagem;
import br.com.fiap.vigisus.domain.triagem.PriorizacaoTriagemPolicy;
import br.com.fiap.vigisus.dto.ComparativoEstadoDTO;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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
        service = new TriagemService(
                perfilService,
                encaminhamentoService,
                iaService,
                new CalculadoraScoreTriagem(),
                new PriorizacaoTriagemPolicy()
        );
    }

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
        when(encaminhamentoService.buscarHospitais(anyString(), anyString(), anyInt())).thenReturn(EncaminhamentoResponse.builder()
                .coIbge(CO_IBGE)
                .municipioOrigem("Lavras")
                .tpLeito("81")
                .hospitais(List.of(
                        EncaminhamentoResponse.HospitalDTO.builder()
                                .coCnes("CNES001")
                                .noFantasia("Hospital Central")
                                .distanciaKm(5.0)
                                .build()))
                .build());
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientacao de triagem.");

        TriagemResponse resp = service.avaliar(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre", "dor_abdominal", "sangramento", "vomito"))
                .comorbidades(List.of("diabetes"))
                .diasSintomas(3)
                .idade(40)
                .build());

        assertThat(resp.getPrioridade()).isEqualTo("CRITICA");
        assertThat(resp.getCorProtocolo()).isEqualTo("VERMELHO");
        assertThat(resp.getEncaminhamento()).isNotNull();
        assertThat(resp.isRequerObservacao()).isTrue();
    }

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
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientacao.");

        TriagemResponse resp = service.avaliar(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre", "dor_muscular", "cefaleia"))
                .comorbidades(List.of())
                .diasSintomas(2)
                .idade(25)
                .build());

        assertThat(resp.getPrioridade()).isEqualTo("MEDIA");
        assertThat(resp.getCorProtocolo()).isEqualTo("AMARELO");
    }

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
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("Orientacao.");

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

        double scoreEpidemia = service.calcularScore(reqEpidemia) * service.resolverMultiplicador("EPIDEMIA");
        double scoreBaixo = service.calcularScore(reqBaixo) * service.resolverMultiplicador("BAIXO");

        assertThat(scoreEpidemia).isGreaterThan(scoreBaixo);

        TriagemResponse respEpidemia = service.avaliar(reqEpidemia);
        TriagemResponse respBaixo = service.avaliar(reqBaixo);

        assertThat(priorityRank(respEpidemia.getPrioridade()))
                .isGreaterThanOrEqualTo(priorityRank(respBaixo.getPrioridade()));
    }

    @Test
    void avaliar_quandoPerfilEIaFalham_retornaFallbackSeguro() {
        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenThrow(new RuntimeException("sem dados"));
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenThrow(new RuntimeException("sem ia"));

        TriagemResponse resp = service.avaliar(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(null)
                .comorbidades(null)
                .diasSintomas(1)
                .idade(30)
                .build());

        assertThat(resp.getPrioridade()).isEqualTo("BAIXA");
        assertThat(resp.getCorProtocolo()).isEqualTo("VERDE");
        assertThat(resp.getAlertaEpidemiologico()).contains("Situação epidemiológica MODERADO").contains(CO_IBGE);
        assertThat(resp.getRecomendacao()).contains("cuidados em domicílio");
        assertThat(resp.getSinaisAlarme()).hasSize(7);
        assertThat(resp.isRequerObservacao()).isFalse();
        assertThat(resp.getEncaminhamento()).isNull();
        assertThat(resp.getTextoIa()).isNull();
        verifyNoInteractions(encaminhamentoService);
    }

    @Test
    void avaliar_quandoPrioridadeAltaETemFalhaNoHospital_mantemRespostaSemEncaminhamento() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .coIbge(CO_IBGE)
                .municipio("Lavras")
                .uf("MG")
                .ano(2024)
                .total(180L)
                .classificacao("ALTO")
                .build();
        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenReturn(perfil);
        when(encaminhamentoService.buscarHospitais(anyString(), anyString(), anyInt())).thenThrow(new RuntimeException("sem vaga"));
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("texto");

        TriagemResponse resp = service.avaliar(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("dor_abdominal", "sangramento"))
                .comorbidades(List.of())
                .diasSintomas(2)
                .idade(30)
                .build());

        assertThat(resp.getPrioridade()).isEqualTo("ALTA");
        assertThat(resp.getCorProtocolo()).isEqualTo("LARANJA");
        assertThat(resp.isRequerObservacao()).isTrue();
        assertThat(resp.getEncaminhamento()).isNull();
        assertThat(resp.getAlertaEpidemiologico()).contains("Situação de ALERTA em Lavras");
        assertThat(resp.getRecomendacao()).contains("sinais de alarme");
    }

    @Test
    void avaliar_quandoEpidemiaComComparativo_destacaPosicaoNoAlerta() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .coIbge(CO_IBGE)
                .municipio("Lavras")
                .uf("MG")
                .ano(2024)
                .total(400L)
                .incidencia(350.0)
                .classificacao("EPIDEMIA")
                .comparativoEstado(ComparativoEstadoDTO.builder().posicaoRankingEstado("3 de 50").build())
                .build();
        when(perfilService.gerarPerfil(eq(CO_IBGE), eq("dengue"), anyInt())).thenReturn(perfil);
        when(iaService.gerarTextoTriagem(anyString(), any(), anyString())).thenReturn("texto");

        TriagemResponse resp = service.avaliar(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("febre"))
                .comorbidades(List.of())
                .diasSintomas(1)
                .idade(30)
                .build());

        assertThat(resp.getAlertaEpidemiologico()).contains("EPIDEMIA de dengue");
        assertThat(resp.getAlertaEpidemiologico()).contains("3º município mais afetado em MG");
    }

    @Test
    void calcularScore_consideraIdadeDiasComorbidadesESintomas() {
        double score = service.calcularScore(TriagemRequest.builder()
                .municipio(CO_IBGE)
                .sintomas(List.of("falta_ar", "febre", "tontura"))
                .comorbidades(List.of("diabetes", "hipertensao"))
                .diasSintomas(5)
                .idade(70)
                .build());

        assertThat(score).isEqualTo(11.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0,BAIXA",
            "2.99,BAIXA",
            "3.0,MEDIA",
            "5.0,MEDIA",
            "5.01,ALTA",
            "8.0,ALTA",
            "8.01,CRITICA"
    })
    void classificarPrioridade_respeitaLimiares(double scoreFinal, String esperado) {
        assertThat(service.classificarPrioridade(scoreFinal)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "EPIDEMIA,1.5",
            "ALTO,1.2",
            "BAIXO,0.8",
            "MODERADO,1.0"
    })
    void resolverMultiplicador_retornaValorEsperado(String classificacao, double esperado) {
        assertThat(service.resolverMultiplicador(classificacao)).isEqualTo(esperado);
    }

    @Test
    void resolverMultiplicador_quandoNulo_retornaNeutro() {
        assertThat(service.resolverMultiplicador(null)).isEqualTo(1.0);
    }

    private int priorityRank(String prioridade) {
        return switch (prioridade) {
            case "CRITICA" -> 4;
            case "ALTA" -> 3;
            case "MEDIA" -> 2;
            default -> 1;
        };
    }
}
