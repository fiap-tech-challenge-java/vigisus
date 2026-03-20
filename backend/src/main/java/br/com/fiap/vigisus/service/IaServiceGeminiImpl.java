package br.com.fiap.vigisus.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Year;
import java.util.List;

@Slf4j
public class IaServiceGeminiImpl implements IaService {

    private final Client client;
    private final String model;
    private final ObjectMapper objectMapper;

    public IaServiceGeminiImpl(String apiKey, String model) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────
    // Método auxiliar central — toda chamada passa aqui
    // ─────────────────────────────────────────────────
    private String chamarGemini(String prompt) {
        try {
            GenerateContentResponse response =
                client.models.generateContent(model, prompt, null);
            String texto = response.text();
            log.debug("Gemini respondeu: {} chars", texto != null ? texto.length() : 0);
            return texto != null ? texto.trim() : "";
        } catch (Exception e) {
            log.error("Erro ao chamar Gemini: {}", e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────────────
    // 1. Texto epidemiológico
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        String prompt = String.format("""
            Você é um sistema de informação em saúde pública do Brasil.
            Com base nos dados reais abaixo, escreva um resumo informativo
            em no máximo 4 linhas para um profissional de saúde.
            NÃO use verbos imperativos. NÃO tome decisões.
            Apenas organize o contexto de forma clara e objetiva.
            Use linguagem simples, sem jargões técnicos.
            IMPORTANTE: use apenas os dados fornecidos, não invente nada.

            Município: %s - %s
            Doença: %s
            Ano: %d
            Total de casos: %d
            Incidência por 100 mil habitantes: %.1f
            Classificação: %s
            """,
            perfil.getMunicipio(),
            perfil.getUf(),
            perfil.getDoenca(),
            perfil.getAno(),
            perfil.getTotal(),
            perfil.getIncidencia(),
            perfil.getClassificacao()
        );

        String resultado = chamarGemini(prompt);
        if (resultado.isBlank()) {
            return gerarTextoEpidemiologicoFallback(perfil);
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────
    // 2. Texto de risco climático
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        String fatores = previsao.getFatores() != null
            ? String.join(", ", previsao.getFatores())
            : "não informados";

        String prompt = String.format("""
            Você é um sistema de informação em saúde pública do Brasil.
            Apresente o contexto de risco epidemiológico de forma objetiva,
            em no máximo 3 linhas, como um briefing informativo.
            NÃO use verbos imperativos. NÃO recomende ações.
            NÃO tome decisões. Apenas organize o contexto.
            IMPORTANTE: use apenas os dados fornecidos, não invente nada.

            Município: %s
            Score de risco: %d/8
            Classificação: %s
            Fatores identificados: %s
            """,
            previsao.getMunicipio(),
            previsao.getScore(),
            previsao.getClassificacao(),
            fatores
        );

        String resultado = chamarGemini(prompt);
        if (resultado.isBlank()) {
            return gerarTextoRiscoFallback(previsao);
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────
    // 3. Interpretação de pergunta em linguagem natural
    // ─────────────────────────────────────────────────
    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        String prompt = String.format("""
            Extraia informações de uma pergunta sobre saúde pública no Brasil.
            Retorne APENAS um JSON válido, sem markdown, sem blocos de código,
            sem explicação adicional. Apenas o JSON puro.

            Formato exato (não altere os nomes dos campos):
            {
              "municipio": "nome do município ou null",
              "uf": "sigla do estado com 2 letras maiúsculas ou null",
              "doenca": "dengue ou chikungunya ou zika ou null",
              "ano": 2024
            }

            Regras:
            - Se não encontrar o município, use null
            - Se não encontrar o estado, use null
            - Se não encontrar a doença, use "dengue" como padrão
            - Se não encontrar o ano, use %d (ano atual)
            - A sigla do estado deve ter exatamente 2 letras maiúsculas

            Pergunta: %s
            """,
            Year.now().getValue(),
            pergunta
        );

        String resposta = chamarGemini(prompt);

        try {
            // Remove possíveis marcadores markdown que o Gemini às vezes inclui
            String json = resposta
                .replace("```json", "")
                .replace("```", "")
                .trim();
            return objectMapper.readValue(json, IntencaoDTO.class);
        } catch (Exception e) {
            log.warn("Falha ao parsear JSON do Gemini: {} | Resposta: {}",
                     e.getMessage(), resposta);
            return IntencaoDTO.builder()
                .doenca("dengue")
                .ano(Year.now().getValue())
                .build();
        }
    }

    // ─────────────────────────────────────────────────
    // 4. Texto de triagem
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoTriagem(String prioridade, List<String> sintomas, String alertaEpidemiologico) {
        String prompt = String.format("""
            Você é um sistema de informação em saúde pública do Brasil.
            Com base nos dados abaixo, escreva uma orientação clara
            em no máximo 3 linhas para o profissional de saúde.
            NÃO diagnostique. Contextualize o risco de forma objetiva.
            IMPORTANTE: use apenas os dados fornecidos, não invente nada.

            Prioridade: %s
            Sintomas: %s
            Contexto epidemiológico: %s
            """,
            prioridade,
            sintomas,
            alertaEpidemiologico
        );

        String resultado = chamarGemini(prompt);
        if (resultado.isBlank()) {
            return String.format("Prioridade %s. Sintomas relatados: %s. %s",
                prioridade, sintomas, alertaEpidemiologico);
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────
    // 5. Texto operacional
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoOperacional(String contexto) {
        String prompt = String.format("""
            Você é um sistema de informação em saúde pública do Brasil.
            Escreva um briefing operacional direto em no máximo 5 linhas
            para o gestor da unidade de saúde.
            Inclua: situação atual, tendência e contexto relevante.
            NÃO use jargões. Seja objetivo e factual.
            IMPORTANTE: use apenas os dados fornecidos, não invente nada.

            Contexto: %s
            """,
            contexto
        );

        String resultado = chamarGemini(prompt);
        if (resultado.isBlank()) {
            return "Briefing operacional: " + contexto;
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────
    // Fallbacks internos (usados se Gemini retornar vazio)
    // ─────────────────────────────────────────────────
    private String gerarTextoEpidemiologicoFallback(PerfilEpidemiologicoResponse perfil) {
        return String.format(
            "Em %d, %s/%s registrou %d casos de %s, " +
            "com incidência de %.1f por 100 mil habitantes. " +
            "Situação classificada como %s.",
            perfil.getAno(), perfil.getMunicipio(), perfil.getUf(),
            perfil.getTotal(), perfil.getDoenca(),
            perfil.getIncidencia(), perfil.getClassificacao()
        );
    }

    private String gerarTextoRiscoFallback(PrevisaoRiscoResponse previsao) {
        return String.format(
            "O contexto epidemiológico de %s apresenta risco %s " +
            "para as próximas 2 semanas (score %d/8).",
            previsao.getMunicipio(),
            previsao.getClassificacao(),
            previsao.getScore()
        );
    }
}
