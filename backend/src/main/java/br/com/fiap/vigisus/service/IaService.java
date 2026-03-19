package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.openai.OpenAiRequest;
import br.com.fiap.vigisus.dto.openai.OpenAiRequest.OpenAiMessage;
import br.com.fiap.vigisus.dto.openai.OpenAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IaService {

    @Value("${vigisus.ia.api-key:}")
    private String apiKey;

    @Value("${vigisus.ia.model:gpt-4o-mini}")
    private String model;

    @Value("${vigisus.ia.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        String perfilJson = toJson(perfil);
        String prompt = "Você é um epidemiologista. Com base nos dados reais abaixo, " +
                "escreva um parágrafo claro e direto explicando a situação " +
                "epidemiológica para um gestor municipal. Não invente dados. " +
                "Dados: " + perfilJson;
        return chamarIa(prompt);
    }

    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        String previsaoJson = toJson(previsao);
        String prompt = "Você é um epidemiologista. Com base nos dados de risco climático abaixo, " +
                "escreva um parágrafo claro e direto explicando os fatores de risco identificados " +
                "e as recomendações para um gestor municipal. Não invente dados. " +
                "Dados: " + previsaoJson;
        return chamarIa(prompt);
    }

    public IntencaoDTO interpretarPergunta(String pergunta) {
        String prompt = "Extraia do texto: municipio (nome), uf (sigla), " +
                "doenca (dengue/chikungunya/zika), ano (int). " +
                "Retorne APENAS JSON válido. " +
                "Texto: " + pergunta;
        String resposta = chamarIa(prompt);
        try {
            return objectMapper.readValue(resposta, IntencaoDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao interpretar resposta da IA: " + e.getMessage(), e);
        }
    }

    private String chamarIa(String prompt) {
        OpenAiRequest request = OpenAiRequest.builder()
                .model(model)
                .messages(List.of(OpenAiMessage.builder()
                        .role("user")
                        .content(prompt)
                        .build()))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);

        OpenAiResponse response = restTemplate.postForObject(apiUrl, entity, OpenAiResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("Resposta inválida da API de IA");
        }

        return response.getChoices().get(0).getMessage().getContent();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar objeto para JSON", e);
        }
    }
}
