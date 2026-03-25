package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IaServiceGeminiImplTest {

    @Mock
    private Client client;

    @Mock
    private Models models;

    @Mock
    private GenerateContentResponse response;

    private IaServiceGeminiImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "models", models);
        service = new IaServiceGeminiImpl(client, "gemini-test");
    }

    @Test
    void gerarTextoOperacional_retornaRespostaDoGemini() {
        when(models.generateContent(anyString(), anyString(), isNull())).thenReturn(response);
        when(response.text()).thenReturn("briefing sintetico");

        String texto = service.gerarTextoOperacional("contexto operacional");

        assertThat(texto).isEqualTo("briefing sintetico");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(models).generateContent(org.mockito.ArgumentMatchers.eq("gemini-test"), captor.capture(), isNull());
        assertThat(captor.getValue()).contains("CONTEXTO").contains("contexto operacional");
    }

    @Test
    void gerarTextoEpidemiologico_fazFallbackQuandoGeminiVemEmBranco() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(80)
                .incidencia(90.0)
                .classificacao("MODERADO")
                .tendencia("ESTAVEL")
                .semanas(List.of(SemanaDTO.builder().semanaEpi(1).casos(10).build()))
                .semanasAnoAnterior(List.of(SemanaDTO.builder().semanaEpi(1).casos(5).build()))
                .build();
        when(models.generateContent(anyString(), anyString(), isNull())).thenReturn(response);
        when(response.text()).thenReturn("   ");

        String texto = service.gerarTextoEpidemiologico(perfil);

        assertThat(texto).contains("Lavras/MG").contains("80 casos de dengue");
    }

    @Test
    void gerarTextoRisco_fazFallbackQuandoGeminiFalha() {
        when(models.generateContent(anyString(), anyString(), isNull())).thenThrow(new RuntimeException("boom"));

        String texto = service.gerarTextoRisco(PrevisaoRiscoResponse.builder()
                .municipio("Lavras")
                .score(5)
                .classificacao("ALTO")
                .fatores(List.of("chuva"))
                .build());

        assertThat(texto).contains("Lavras apresenta risco alto").contains("chuva");
    }

    @Test
    void interpretarPergunta_removeMarkdownERetornaDto() {
        when(models.generateContent(anyString(), anyString(), isNull())).thenReturn(response);
        when(response.text()).thenReturn("```json\n{\"municipio\":\"Lavras\",\"uf\":\"MG\",\"doenca\":\"dengue\",\"ano\":2024}\n```");

        IntencaoDTO dto = service.interpretarPergunta("Como esta Lavras?");

        assertThat(dto.getMunicipio()).isEqualTo("Lavras");
        assertThat(dto.getUf()).isEqualTo("MG");
        assertThat(dto.getDoenca()).isEqualTo("dengue");
        assertThat(dto.getAno()).isEqualTo(2024);
    }

    @Test
    void interpretarPergunta_fazFallbackQuandoNaoConsegueParsear() {
        when(models.generateContent(anyString(), anyString(), isNull())).thenReturn(response);
        when(response.text()).thenReturn("resposta invalida");

        IntencaoDTO dto = service.interpretarPergunta("Pergunta ruim");

        assertThat(dto.getDoenca()).isEqualTo("dengue");
        assertThat(dto.getAno()).isEqualTo(Year.now().getValue());
        assertThat(dto.getMunicipio()).isNull();
    }

    @Test
    void gerarTextoTriagem_montaTextoPadraoQuandoGeminiNaoRetornaConteudo() {
        when(models.generateContent(anyString(), anyString(), isNull())).thenReturn(response);
        when(response.text()).thenReturn("");

        String texto = service.gerarTextoTriagem("VERMELHO", List.of("febre", "dor"), "alto risco");

        assertThat(texto).contains("Prioridade VERMELHO").contains("febre").contains("alto risco");
    }
}
