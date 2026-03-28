package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.application.triagem.AvaliarTriagemUseCase;
import br.com.fiap.vigisus.application.triagem.ConsultarCatalogoTriagemUseCase;
import br.com.fiap.vigisus.dto.TriagemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TriagemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvaliarTriagemUseCase avaliarTriagemUseCase;

    @MockBean
    private ConsultarCatalogoTriagemUseCase consultarCatalogoTriagemUseCase;

    @Test
    void deveRetornar200ParaTriagemValida() throws Exception {
        String requestBody = """
                {
                  "municipio": "3550308",
                  "sintomas": ["febre", "dor_muscular"],
                  "diasSintomas": 3,
                  "idade": 30,
                  "comorbidades": []
                }
                """;

        when(avaliarTriagemUseCase.executar(any())).thenReturn(
                TriagemResponse.builder()
                        .prioridade("AMARELO")
                        .recomendacao("Busque atendimento médico em até 24 horas")
                        .build());

        mockMvc.perform(post("/api/triagem/avaliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").exists())
                .andExpect(jsonPath("$.recomendacao").exists());
    }

    @Test
    void deveRetornar200ParaCatalogoSintomas() throws Exception {
        when(consultarCatalogoTriagemUseCase.executar()).thenReturn(
                Map.of(
                        "sintomas", List.of("febre", "dor_muscular"),
                        "comorbidades", List.of("diabetes", "hipertensao")));

        mockMvc.perform(get("/api/triagem/sintomas-validos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sintomas").isArray())
                .andExpect(jsonPath("$.sintomas[0]").isNotEmpty());
    }

    @Test
    void deveRetornar400ParaRequestSemCamposObrigatorios() throws Exception {
        String requestBody = """
                {
                  "sintomas": [],
                  "idade": 30
                }
                """;

        mockMvc.perform(post("/api/triagem/avaliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void deveRetornar400ParaSintomasVazios() throws Exception {
        String requestBody = """
                {
                  "municipio": "Campinas",
                  "sintomas": [],
                  "diasSintomas": 3,
                  "idade": 30,
                  "comorbidades": []
                }
                """;

        mockMvc.perform(post("/api/triagem/avaliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos").isArray());
    }
}
