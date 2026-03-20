package br.com.fiap.vigisus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.Year;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
public class IaServiceGeminiImpl implements IaService {

    private final Client geminiClient;
    private final String model;
    private final ObjectMapper objectMapper;

    // Semáforo: 1 chamada concorrente para não estourar quota gratuita
    // Gemini free tier: 15 RPM — com semaphore(1) fica seguro
    private final Semaphore semaphore = new Semaphore(1);

    public IaServiceGeminiImpl(
            Client geminiClient,
            @Value("${vigisus.ia.model:gemini-2.5-flash}") String model) {
        this.geminiClient = geminiClient;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        log.info("[IaService] Gemini inicializado com modelo: {}", model);
    }

    // ─────────────────────────────────────────────────
    // Método central — toda chamada passa aqui
    // ─────────────────────────────────────────────────
    private String chamarGemini(String prompt) {
        try {
            semaphore.acquire();
            try {
                log.info("[Gemini] Enviando prompt ({} chars)", prompt.length());

                var response = geminiClient.models.generateContent(
                    model,
                    prompt,
                    null
                );

                String texto = response.text();
                log.info("[Gemini] Resposta recebida ({} chars)",
                    texto != null ? texto.length() : 0);

                return texto != null ? texto.trim() : "";

            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Gemini] Thread interrompida: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("[Gemini] Erro ao chamar API: {}", e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────────────
    // 1. Texto epidemiológico
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        String prompt = String.format(
            "Você é um sistema de informação em saúde pública do Brasil. " +
            "Com base nos dados reais abaixo, escreva um resumo informativo " +
            "em no máximo 4 linhas para um profissional de saúde. " +
            "NÃO use verbos imperativos. NÃO tome decisões. " +
            "Apenas organize o contexto de forma clara e objetiva. " +
            "Use linguagem simples, sem jargões. " +
            "IMPORTANTE: use apenas os dados fornecidos, não invente nada.\n\n" +
            "Município: %s - %s\n" +
            "Doença: %s\n" +
            "Ano: %d\n" +
            "Total de casos: %d\n" +
            "Incidência por 100 mil habitantes: %.1f\n" +
            "Classificação: %s",
            perfil.getMunicipio(),
            perfil.getUf(),
            perfil.getDoenca(),
            perfil.getAno(),
            perfil.getTotal(),
            perfil.getIncidencia(),
            perfil.getClassificacao()
        );

        String resultado = chamarGemini(prompt);
        return resultado.isBlank() ? fallbackEpidemiologico(perfil) : resultado;
    }

    // ─────────────────────────────────────────────────
    // 2. Texto de risco climático
    // ─────────────────────────────────────────────────
    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        String fatores = previsao.getFatores() != null
            ? String.join(", ", previsao.getFatores())
            : "não informados";

        String prompt = String.format(
            "Você é um sistema de informação em saúde pública do Brasil. " +
            "Apresente o contexto de risco epidemiológico de forma objetiva, " +
            "em no máximo 3 linhas, como um briefing informativo. " +
            "NÃO use verbos imperativos. NÃO recomende ações. " +
            "NÃO tome decisões. Apenas organize o contexto. " +
            "IMPORTANTE: use apenas os dados fornecidos, não invente nada.\n\n" +
            "Município: %s\n" +
            "Score de risco: %d/8\n" +
            "Classificação: %s\n" +
            "Fatores identificados: %s",
            previsao.getMunicipio(),
            previsao.getScore(),
            previsao.getClassificacao(),
            fatores
        );

        String resultado = chamarGemini(prompt);
        return resultado.isBlank() ? fallbackRisco(previsao) : resultado;
    }

    // ─────────────────────────────────────────────────
    // 3. Interpretação de linguagem natural
    // ─────────────────────────────────────────────────
    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        String prompt = String.format(
            "Extraia informações de uma pergunta sobre saúde pública no Brasil. " +
            "Responda APENAS com um JSON válido e compacto, sem markdown, " +
            "sem bloco de código, sem explicação. Apenas o JSON puro.\n\n" +
            "Schema obrigatório:\n" +
            "{\"municipio\":\"nome ou null\",\"uf\":\"sigla 2 letras maiúsculas ou null\"," +
            "\"doenca\":\"dengue ou chikungunya ou zika ou null\",\"ano\":2024}\n\n" +
            "Regras:\n" +
            "- Se não encontrar o município: null\n" +
            "- Se não encontrar o estado: null\n" +
            "- Se não encontrar a doença: \"dengue\"\n" +
            "- Se não encontrar o ano: %d\n" +
            "- UF deve ter exatamente 2 letras maiúsculas\n\n" +
            "Pergunta: %s",
            Year.now().getValue(),
            pergunta
        );

        String resposta = chamarGemini(prompt);

        try {
            // Remove markdown que o Gemini às vezes inclui mesmo pedindo para não incluir
            String json = resposta
                .replace("```json", "")
                .replace("```", "")
                .trim();

            return objectMapper.readValue(json, IntencaoDTO.class);

        } catch (Exception e) {
            log.warn("[Gemini] Falha ao parsear JSON: {} | Resposta bruta: {}",
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
        String prompt = String.format(
            "Você é um sistema de informação em saúde pública do Brasil. " +
            "Com base nos dados abaixo, escreva uma orientação clara " +
            "em no máximo 3 linhas para o profissional de saúde. " +
            "NÃO diagnostique. Contextualize o risco de forma objetiva. " +
            "IMPORTANTE: use apenas os dados fornecidos, não invente nada.\n\n" +
            "Prioridade: %s\n" +
            "Sintomas: %s\n" +
            "Contexto epidemiológico: %s",
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
        String prompt = String.format(
            "Você é um sistema de informação em saúde pública do Brasil. " +
            "Escreva um briefing operacional direto em no máximo 5 linhas " +
            "para o gestor da unidade de saúde. " +
            "Inclua: situação atual, tendência e contexto relevante. " +
            "NÃO use jargões. Seja objetivo e factual. " +
            "IMPORTANTE: use apenas os dados fornecidos, não invente nada.\n\n" +
            "Contexto: %s",
            contexto
        );

        String resultado = chamarGemini(prompt);
        if (resultado.isBlank()) {
            return "Briefing operacional: " + contexto;
        }
        return resultado;
    }

    // ─────────────────────────────────────────────────
    // Fallbacks — usados quando Gemini retorna vazio
    // ─────────────────────────────────────────────────
    private String fallbackEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        return String.format(
            "Em %d, %s/%s registrou %d casos de %s, " +
            "com incidência de %.1f por 100 mil habitantes. " +
            "Situação classificada como %s.",
            perfil.getAno(), perfil.getMunicipio(), perfil.getUf(),
            perfil.getTotal(), perfil.getDoenca(),
            perfil.getIncidencia(), perfil.getClassificacao()
        );
    }

    private String fallbackRisco(PrevisaoRiscoResponse previsao) {
        return String.format(
            "Contexto epidemiológico de %s: risco %s para as próximas 2 semanas (score %d/8).",
            previsao.getMunicipio(),
            previsao.getClassificacao(),
            previsao.getScore()
        );
    }
}
