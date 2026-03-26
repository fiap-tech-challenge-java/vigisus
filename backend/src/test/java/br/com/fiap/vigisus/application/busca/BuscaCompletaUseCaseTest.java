package br.com.fiap.vigisus.application.busca;

import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.MunicipioService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscaCompletaUseCaseTest {

    private IaService iaService;
    private PerfilEpidemiologicoService perfilService;
    private PrevisaoRiscoService previsaoRiscoService;
    private EncaminhamentoService encaminhamentoService;
    private MunicipioService municipioService;
    private BuscaCompletaUseCase useCase;

    @BeforeEach
    void setUp() {
        iaService = mock(IaService.class);
        perfilService = mock(PerfilEpidemiologicoService.class);
        previsaoRiscoService = mock(PrevisaoRiscoService.class);
        encaminhamentoService = mock(EncaminhamentoService.class);
        municipioService = mock(MunicipioService.class);
        useCase = new BuscaCompletaUseCase(
                iaService,
                perfilService,
                previsaoRiscoService,
                encaminhamentoService,
                municipioService
        );
    }

    @Test
    void buscarPorPergunta_retornaRespostaCompletaComInterpretacao() {
        IntencaoDTO intencao = IntencaoDTO.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .build();
        Municipio municipio = Municipio.builder().coIbge("3131307").noMunicipio("Lavras").sgUf("MG").build();
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(100L)
                .incidencia(50.0)
                .classificacao("MODERADO")
                .build();
        PrevisaoRiscoResponse risco = PrevisaoRiscoResponse.builder()
                .coIbge("3131307")
                .score(4)
                .classificacao("ALTO")
                .fatores(List.of("chuva"))
                .build();
        EncaminhamentoResponse encaminhamento = EncaminhamentoResponse.builder()
                .coIbge("3131307")
                .hospitais(List.of(EncaminhamentoResponse.HospitalDTO.builder()
                        .noFantasia("Hospital A")
                        .distanciaKm(5.0)
                        .qtLeitosSus(10)
                        .nuTelefone("9999")
                        .build()))
                .build();

        when(iaService.interpretarPergunta("dengue em Lavras MG 2024")).thenReturn(intencao);
        when(municipioService.buscarPorNomeEUf("Lavras", "MG")).thenReturn(List.of(municipio));
        when(perfilService.gerarPerfil("3131307", "dengue", 2024)).thenReturn(perfil);
        when(previsaoRiscoService.calcularRisco("3131307")).thenReturn(risco);
        when(encaminhamentoService.buscarHospitais("3131307", "74", 5)).thenReturn(encaminhamento);
        when(iaService.gerarTextoOperacional(anyString())).thenReturn("briefing");

        BuscaCompletaResponse response = useCase.buscarPorPergunta("dengue em Lavras MG 2024");

        assertThat(response.getTextoIa()).isEqualTo("briefing");
        assertThat(response.getPerfil()).isEqualTo(perfil);
        assertThat(response.getRisco()).isEqualTo(risco);
        assertThat(response.getEncaminhamento()).isEqualTo(encaminhamento);
        assertThat(response.getInterpretacao().getCoIbge()).isEqualTo("3131307");
    }

    @Test
    void buscarPorPergunta_aplicaFallbacksQuandoDependenciasFalham() {
        IntencaoDTO intencao = IntencaoDTO.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .build();
        Municipio municipio = Municipio.builder().coIbge("3131307").noMunicipio("Lavras").sgUf("MG").build();
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(100L)
                .incidencia(50.0)
                .classificacao("MODERADO")
                .build();

        when(iaService.interpretarPergunta("dengue em Lavras MG 2024")).thenReturn(intencao);
        when(municipioService.buscarPorNomeEUf("Lavras", "MG")).thenReturn(List.of(municipio));
        when(perfilService.gerarPerfil("3131307", "dengue", 2024)).thenReturn(perfil);
        when(previsaoRiscoService.calcularRisco("3131307")).thenThrow(new RuntimeException("sem clima"));
        when(encaminhamentoService.buscarHospitais("3131307", "74", 5)).thenThrow(new RuntimeException("sem hospital"));
        when(iaService.gerarTextoOperacional(anyString())).thenThrow(new RuntimeException("sem ia"));

        BuscaCompletaResponse response = useCase.buscarPorPergunta("dengue em Lavras MG 2024");

        assertThat(response.getRisco().getClassificacao()).isEqualTo("INDISPONIVEL");
        assertThat(response.getEncaminhamento().getHospitais()).isEmpty();
        assertThat(response.getTextoIa()).contains("Lavras").contains("100 casos");
    }

    @Test
    void buscarPorPergunta_lancaQuandoMunicipioNaoIdentificado() {
        when(iaService.interpretarPergunta("pergunta vaga")).thenReturn(IntencaoDTO.builder().uf("MG").build());

        assertThatThrownBy(() -> useCase.buscarPorPergunta("pergunta vaga"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Município");
    }

    @Test
    void buscarDireto_lancaQuandoMunicipioNaoEncontrado() {
        when(municipioService.buscarPorNomeEUf("Lavras", "MG")).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.buscarDireto("Lavras", "MG", "dengue", 2024))
                .isInstanceOf(MunicipioNotFoundException.class);
    }

    @Test
    void buscarPorPergunta_usaBuscaPorNomeQuandoUfNaoEncontraCandidato() {
        IntencaoDTO intencao = IntencaoDTO.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .build();
        Municipio municipio = Municipio.builder().coIbge("3131307").noMunicipio("Lavras").sgUf("MG").build();
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(1L)
                .incidencia(1.0)
                .classificacao("BAIXO")
                .build();

        when(iaService.interpretarPergunta("dengue em Lavras MG 2024")).thenReturn(intencao);
        when(municipioService.buscarPorNomeEUf("Lavras", "MG")).thenReturn(List.of());
        when(municipioService.buscarPorNome("Lavras")).thenReturn(Optional.of(municipio));
        when(perfilService.gerarPerfil("3131307", "dengue", 2024)).thenReturn(perfil);
        when(previsaoRiscoService.calcularRisco("3131307")).thenReturn(
                PrevisaoRiscoResponse.builder().score(0).classificacao("BAIXO").fatores(List.of()).build()
        );
        when(encaminhamentoService.buscarHospitais("3131307", "74", 5)).thenReturn(
                EncaminhamentoResponse.builder().coIbge("3131307").hospitais(List.of()).build()
        );
        when(iaService.gerarTextoOperacional(anyString())).thenReturn("ok");

        BuscaCompletaResponse response = useCase.buscarPorPergunta("dengue em Lavras MG 2024");

        assertThat(response.getInterpretacao().getCoIbge()).isEqualTo("3131307");
    }

    @Test
    void buscarPorPergunta_lancaQuandoUfNaoIdentificada() {
        when(iaService.interpretarPergunta("lavras")).thenReturn(IntencaoDTO.builder()
                .municipio("Lavras")
                .build());

        assertThatThrownBy(() -> useCase.buscarPorPergunta("lavras"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("UF");
    }
}
