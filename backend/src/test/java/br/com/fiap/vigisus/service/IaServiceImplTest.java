package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.openai.OpenAiRequest;
import br.com.fiap.vigisus.dto.openai.OpenAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IaServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private IaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new IaServiceImpl(
                "secret-key",
                "gpt-test",
                "https://example.test/chat",
                321,
                0.4,
                restTemplate,
                objectMapper
        );
    }

    @Test
    void gerarTextoEpidemiologico_enviaRequestEsperadaERetornaConteudo() {
        PerfilEpidemiologicoResponse perfil = PerfilEpidemiologicoResponse.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .total(123L)
                .incidencia(45.6)
                .classificacao("MODERADO")
                .build();
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(responseComTexto("analise"));

        String texto = service.gerarTextoEpidemiologico(perfil);

        assertThat(texto).isEqualTo("analise");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<OpenAiRequest>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).postForObject(eq("https://example.test/chat"), captor.capture(), eq(OpenAiResponse.class));

        HttpEntity<OpenAiRequest> entity = captor.getValue();
        assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer secret-key");
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getModel()).isEqualTo("gpt-test");
        assertThat(entity.getBody().getMaxTokens()).isEqualTo(321);
        assertThat(entity.getBody().getTemperature()).isEqualTo(0.4);
        assertThat(entity.getBody().getMessages()).hasSize(2);
        assertThat(entity.getBody().getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(entity.getBody().getMessages().get(1).getRole()).isEqualTo("user");
        assertThat(entity.getBody().getMessages().get(1).getContent()).contains("Lavras").contains("123");
    }

    @Test
    void interpretarPergunta_retornaDtoQuandoJsonValido() throws Exception {
        IntencaoDTO dto = IntencaoDTO.builder()
                .municipio("Lavras")
                .uf("MG")
                .doenca("dengue")
                .ano(2024)
                .build();
        String json = "{\"municipio\":\"Lavras\",\"uf\":\"MG\",\"doenca\":\"dengue\",\"ano\":2024}";
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(responseComTexto(json));
        when(objectMapper.readValue(json, IntencaoDTO.class)).thenReturn(dto);

        IntencaoDTO result = service.interpretarPergunta("Como esta Lavras?");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void interpretarPergunta_fazFallbackQuandoJsonInvalido() throws Exception {
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(responseComTexto("nao-json"));
        when(objectMapper.readValue("nao-json", IntencaoDTO.class))
                .thenThrow(new JsonProcessingException("erro") {});

        IntencaoDTO result = service.interpretarPergunta("Pergunta ambigua");

        assertThat(result.getDoenca()).isEqualTo("dengue");
        assertThat(result.getAno()).isEqualTo(Year.now().getValue());
        assertThat(result.getMunicipio()).isNull();
        assertThat(result.getUf()).isNull();
    }

    @Test
    void gerarTextoRisco_lancaQuandoRespostaDaIaENula() {
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> service.gerarTextoRisco(PrevisaoRiscoResponse.builder()
                .municipio("Lavras")
                .score(4)
                .classificacao("ALTO")
                .fatores(List.of("chuva"))
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resposta nula da API de IA");
    }

    @Test
    void gerarTextoTriagem_lancaQuandoListaDeChoicesVemVazia() {
        OpenAiResponse response = new OpenAiResponse();
        response.setChoices(List.of());
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> service.gerarTextoTriagem("AMARELO", List.of("febre"), "alerta"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("lista de escolhas vazia");
    }

    @Test
    void gerarTextoOperacional_retornaConteudoDaPrimeiraChoice() {
        when(restTemplate.postForObject(eq("https://example.test/chat"), org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(OpenAiResponse.class)))
                .thenReturn(responseComTexto("briefing pronto"));

        String texto = service.gerarTextoOperacional("contexto resumido");

        assertThat(texto).isEqualTo("briefing pronto");
    }

    private OpenAiResponse responseComTexto(String texto) {
        OpenAiResponse.Message message = new OpenAiResponse.Message();
        message.setRole("assistant");
        message.setContent(texto);

        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(message);

        OpenAiResponse response = new OpenAiResponse();
        response.setChoices(List.of(choice));
        return response;
    }
}
