package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

@Slf4j
public class IaServiceGeminiImpl implements IaService {

    private final Client geminiClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore = new Semaphore(1);

    public IaServiceGeminiImpl(
            Client geminiClient,
            @Value("${vigisus.ia.model:gemini-2.5-flash}") String model) {
        this.geminiClient = geminiClient;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        log.info("[IaService] Gemini inicializado com modelo: {}", model);
    }

    private String chamarGemini(String prompt) {
        try {
            semaphore.acquire();
            try {
                var response = geminiClient.models.generateContent(model, prompt, null);
                String texto = response.text();
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

    @Override
    public String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil) {
        long totalAnterior = TextoAnaliticoHelper.somarCasos(perfil.getSemanasAnoAnterior());

        String prompt = String.format(Locale.US,
                "Voce e um analista senior de vigilancia epidemiologica do SUS. " +
                "Escreva uma analise textual clara, util e complementar aos graficos. " +
                "Traga 3 pequenos paragrafos: " +
                "(1) leitura do cenario, " +
                "(2) o que nao fica obvio olhando apenas os numeros, " +
                "(3) o que merece acompanhamento. " +
                "Nao invente dados, nao use markdown, nao faca listas, nao diagnostique, nao seja vago. " +
                "Use no maximo 900 caracteres e linguagem direta.\n\n" +
                "LOCAL: %s/%s\n" +
                "DOENCA: %s\n" +
                "ANO: %d\n" +
                "TOTAL DE CASOS: %d\n" +
                "INCIDENCIA: %.1f por 100 mil\n" +
                "CLASSIFICACAO: %s\n" +
                "TENDENCIA: %s\n" +
                "TOTAL ANO ANTERIOR (soma da serie semanal): %d\n" +
                "POSICAO ESTADUAL: %s\n" +
                "INCIDENCIA MEDIA DO ESTADO: %s\n\n" +
                "Texto:",
                perfil.getMunicipio(),
                perfil.getUf(),
                perfil.getDoenca(),
                perfil.getAno(),
                perfil.getTotal(),
                perfil.getIncidencia(),
                perfil.getClassificacao(),
                perfil.getTendencia(),
                totalAnterior,
                perfil.getPosicaoEstado() != null ? perfil.getPosicaoEstado() : "indisponivel",
                perfil.getIncidenciaMediaEstado() != null
                        ? String.format(Locale.US, "%.1f", perfil.getIncidenciaMediaEstado())
                        : "indisponivel");

        String resultado = chamarGemini(prompt);
        return resultado.isBlank() ? TextoAnaliticoHelper.montarTextoEpidemiologico(perfil) : resultado;
    }

    @Override
    public String gerarTextoRisco(PrevisaoRiscoResponse previsao) {
        String fatores = previsao.getFatores() != null
                ? String.join("; ", previsao.getFatores())
                : "nao informados";

        String prompt = String.format(
                "Voce e um analista de risco em saude publica. " +
                "Explique o risco para as proximas 2 semanas em 2 paragrafos curtos, " +
                "com linguagem simples, sem listas e sem recomendacoes clinicas. " +
                "No primeiro, diga o nivel geral. No segundo, explique o que sustenta esse risco e como ele complementa a leitura dos casos recentes. " +
                "Nao invente nada.\n\n" +
                "LOCAL: %s\n" +
                "SCORE: %d/8\n" +
                "CLASSIFICACAO: %s\n" +
                "FATORES: %s\n\n" +
                "Texto:",
                previsao.getMunicipio(),
                previsao.getScore(),
                previsao.getClassificacao(),
                fatores
        );

        String resultado = chamarGemini(prompt);
        return resultado.isBlank() ? TextoAnaliticoHelper.montarTextoRisco(previsao) : resultado;
    }

    @Override
    public IntencaoDTO interpretarPergunta(String pergunta) {
        String prompt = String.format(
                "Extraia informacoes de uma pergunta sobre saude publica no Brasil. " +
                "Responda apenas com um JSON valido e compacto, sem markdown.\n\n" +
                "Schema:\n" +
                "{\"municipio\":\"nome ou null\",\"uf\":\"sigla 2 letras ou null\",\"doenca\":\"dengue ou chikungunya ou zika ou null\",\"ano\":2024}\n\n" +
                "Regras:\n" +
                "- se nao houver municipio: null\n" +
                "- se nao houver UF: null\n" +
                "- se nao houver doenca: \"dengue\"\n" +
                "- se nao houver ano: %d\n\n" +
                "Pergunta: %s",
                Year.now().getValue(),
                pergunta
        );

        String resposta = chamarGemini(prompt);
        try {
            String json = resposta.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(json, IntencaoDTO.class);
        } catch (Exception e) {
            log.warn("[Gemini] Falha ao parsear JSON: {}", e.getMessage());
            return IntencaoDTO.builder()
                    .doenca("dengue")
                    .ano(Year.now().getValue())
                    .build();
        }
    }

    @Override
    public String gerarTextoTriagem(String prioridade, List<String> sintomas, String alertaEpidemiologico) {
        String prompt = String.format(
                "Voce apoia a triagem de uma unidade do SUS. " +
                "Escreva uma orientacao em no maximo 4 linhas, sem diagnosticar, " +
                "com foco em gravidade, contexto epidemiologico e necessidade de observacao. " +
                "Use apenas os dados fornecidos.\n\n" +
                "PRIORIDADE: %s\n" +
                "SINTOMAS: %s\n" +
                "CONTEXTO: %s\n\n" +
                "Texto:",
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

    @Override
    public String gerarTextoOperacional(String contexto) {
        String prompt = String.format(
                "Voce escreve briefings operacionais para gestao em saude. " +
                "Produza um texto de 3 pequenos paragrafos: situacao atual, leitura complementar e ponto de atencao imediato. " +
                "Nao use listas, nao use jargoes excessivos, nao invente dados. " +
                "O texto deve complementar o painel, nao repetir mecanicamente cada numero.\n\n" +
                "CONTEXTO:\n%s\n\n" +
                "Texto:",
                contexto
        );

        String resultado = chamarGemini(prompt);
        return resultado.isBlank() ? TextoAnaliticoHelper.montarTextoOperacional(contexto) : resultado;
    }
}
