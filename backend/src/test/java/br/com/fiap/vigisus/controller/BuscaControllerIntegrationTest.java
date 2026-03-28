package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.busca.BuscaCompletaUseCase;
import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BuscaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuscaCompletaUseCase buscaCompletaUseCase;

    @Test
    void deveRetornar200ParaBuscaValida() throws Exception {
        String requestBody = """
                {
                  "pergunta": "Qual a situação da dengue em Campinas este ano?"
                }
                """;

        when(buscaCompletaUseCase.buscarPorPergunta(anyString()))
                .thenReturn(BuscaCompletaResponse.builder().textoIa("briefing").build());

        mockMvc.perform(post("/api/busca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400ParaPerguntaVazia() throws Exception {
        String requestBody = """
                {
                  "pergunta": ""
                }
                """;

        mockMvc.perform(post("/api/busca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void deveRetornar400ParaPerguntaNula() throws Exception {
        String requestBody = """
                {}
                """;

        mockMvc.perform(post("/api/busca")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }
}
