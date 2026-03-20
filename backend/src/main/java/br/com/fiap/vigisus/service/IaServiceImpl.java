package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.openai.OpenAiRequest;
import br.com.fiap.vigisus.dto.openai.OpenAiRequest.OpenAiMessage;
import br.com.fiap.vigisus.dto.openai.OpenAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.List;

public class IaServiceImpl implements IaService {

    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final int maxTokens;
    private final double temperature;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public IaServiceImpl(String apiKey, String model, String apiUrl,
                         int maxTokens, double temperature,
                         RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        String systemPrompt = "Você é um epidemiologista do Ministério da Saúde.\n" +
                "Escreva UM parágrafo claro e direto (máximo 5 linhas)\n" +
                "explicando a situação epidemiológica para um gestor municipal.\n" +
                "Use linguagem simples, sem jargões.\n" +
                "IMPORTANTE: use apenas os dados fornecidos, não invente nada.";

        String userPrompt = String.format(
                "Município: %s - %s%n" +
                "Doença: %s%n" +
                "Ano: %d%n" +
                "Total de casos: %d%n" +
                "Incidência por 100k hab: %.2f%n" +
                "Classificação: %s",
                perfil.getMunicipio(), perfil.getUf(),
                perfil.getDoenca(),
                perfil.getAno(),
                perfil.getTotal(),
                perfil.getIncidencia(),
                perfil.getClassificacao());

        return chamarIa(systemPrompt, userPrompt);
    }

    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        String systemPrompt = "Você é um especialista em vigilância epidemiológica.\n" +
                "Escreva UM parágrafo (máximo 4 linhas) explicando o risco\n" +
                "de dengue para as próximas 2 semanas em linguagem simples.\n" +
                "Use apenas os dados fornecidos.";

        String userPrompt = String.format(
                "Município: %s%n" +
                "Score de risco: %d/8%n" +
                "Classificação: %s%n" +
                "Fatores identificados: %s",
                previsao.getMunicipio(),
                previsao.getScore(),
                previsao.getClassificacao(),
                previsao.getFatores());

        return chamarIa(systemPrompt, userPrompt);
    }

    @Override
    public String gerarTextoOperacional(String contexto) {
        String systemPrompt = "Você é um sistema de informação em saúde pública.\n" +
                "Apresente o contexto epidemiológico de forma objetiva,\n" +
                "em no máximo 4 linhas, como um briefing informativo.\n" +
                "NÃO use verbos imperativos. NÃO recomende ações.\n" +
                "NÃO tome decisões. Apenas organize o contexto para\n" +
                "que o profissional de saúde possa avaliar.";

        return chamarIa(systemPrompt, contexto);
    }

    @Override
    public String gerarTextoBuscaCompleta(String contexto) {
        String systemPrompt = "Você é um sistema de informação em saúde pública.\n" +
                "Com base nos dados abaixo, escreva um resumo informativo\n" +
                "em no máximo 6 linhas para um profissional de saúde.\n" +
                "Cubra: situação atual, tendência de risco e estrutura\n" +
                "hospitalar disponível.\n" +
                "NÃO use verbos imperativos. NÃO tome decisões.\n" +
                "Apenas organize o contexto de forma clara e objetiva.";

        return chamarIa(systemPrompt, contexto);
    }

    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        String systemPrompt = "Extraia informações de uma pergunta sobre saúde pública.\n" +
                "Retorne APENAS um JSON válido, sem markdown, sem explicação.\n" +
                "Formato exato:\n" +
                "{\n" +
                "  \"municipio\": \"nome do município ou null\",\n" +
                "  \"uf\": \"sigla do estado ou null\",\n" +
                "  \"doenca\": \"dengue ou chikungunya ou zika ou null\",\n" +
                "  \"ano\": 2024\n" +
                "}\n" +
                "Se não encontrar algum campo, use null.\n" +
                "Para ano: se não mencionado, use o ano atual.";

        String resposta = chamarIa(systemPrompt, pergunta);
        try {
            return objectMapper.readValue(resposta, IntencaoDTO.class);
        } catch (JsonProcessingException e) {
            return IntencaoDTO.builder()
                    .doenca("dengue")
                    .ano(Year.now().getValue())
                    .municipio(null)
                    .uf(null)
                    .build();
        }
    }

    @Override
    public String gerarTextoTriagem(String prioridade, List<String> sintomas, String alertaEpidemiologico) {
        String systemPrompt = "Você é um enfermeiro triagista experiente do SUS.\n" +
                "Baseado nos dados abaixo, escreva uma orientação clara\n" +
                "(máximo 3 linhas) para o profissional de saúde.\n" +
                "NÃO diagnostique. Contextualize o risco e oriente a conduta.";

        String userPrompt = String.format(
                "Dados: prioridade=%s, sintomas=%s, contexto=%s",
                prioridade, sintomas, alertaEpidemiologico);

        return chamarIa(systemPrompt, userPrompt);
    }

    private String chamarIa(String systemPrompt, String userPrompt) {
        OpenAiRequest request = OpenAiRequest.builder()
                .model(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .messages(List.of(
                        OpenAiMessage.builder().role(SYSTEM_ROLE).content(systemPrompt).build(),
                        OpenAiMessage.builder().role(USER_ROLE).content(userPrompt).build()
                ))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);

        OpenAiResponse response = restTemplate.postForObject(apiUrl, entity, OpenAiResponse.class);

        if (response == null) {
            throw new RuntimeException("Resposta nula da API de IA");
        }
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("A API de IA retornou uma lista de escolhas vazia ou nula");
        }

        return response.getChoices().get(0).getMessage().getContent();
    }
}
