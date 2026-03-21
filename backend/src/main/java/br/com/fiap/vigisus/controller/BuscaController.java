package br.com.fiap.vigisus.controller;

import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.BuscaRequest;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.MunicipioService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/busca")
@Tag(name = "Busca por Linguagem Natural")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuscaController {

    private final IaService iaService;
    private final PerfilEpidemiologicoService perfilService;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
    private final MunicipioService municipioService;

    private static final String TP_LEITO_CLINICO = "74";
    private static final int MIN_HOSPITAIS = 5;

    @PostMapping
    @Operation(
            summary = "Busca completa por linguagem natural",
            description = """
                    Interpreta uma pergunta em português e retorna contexto
                    epidemiológico completo: histórico de casos, risco climático,
                    hospitais com estrutura disponível e resumo narrativo gerado
                    por IA.

                    Exemplo de perguntas:
                      "dengue em Lavras MG 2024"
                      "situação da dengue em Belo Horizonte"
                      "casos de dengue em Varginha nos últimos anos"

                    Retorna contexto informacional baseado em dados públicos do SUS.
                    A IA narra dados existentes. Não realiza diagnóstico.
                    A decisão final permanece com o profissional de saúde habilitado.
                    """)
    public BuscaCompletaResponse buscar(@RequestBody BuscaRequest request) {

        // 1. Interpret question using IA
        IntencaoDTO intencao = iaService.interpretarPergunta(request.getPergunta());

        // 2. Find municipality by name + UF and resolve coIbge
        Municipio municipio = encontrarMunicipio(intencao.getMunicipio(), intencao.getUf());
        String coIbge = municipio.getCoIbge();
        intencao.setCoIbge(coIbge);

        // 3. Resolve year
        int ano = intencao.getAno() != null ? intencao.getAno() : LocalDate.now().getYear();
        String doenca = intencao.getDoenca() != null ? intencao.getDoenca() : "dengue";

        // 4. Fetch perfil, risco and encaminhamento in parallel
        CompletableFuture<PerfilEpidemiologicoResponse> futurePerfil =
                CompletableFuture.supplyAsync(() -> perfilService.gerarPerfil(coIbge, doenca, ano));

        CompletableFuture<PrevisaoRiscoResponse> futureRisco =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return previsaoRiscoService.calcularRisco(coIbge);
                    } catch (Exception e) {
                        log.warn("[Busca] Risco indisponível para {}: {}", coIbge, e.getMessage());
                        return PrevisaoRiscoResponse.builder()
                                .coIbge(coIbge)
                                .score(0)
                                .classificacao("INDISPONIVEL")
                                .fatores(List.of("Previsão climática temporariamente indisponível"))
                                .build();
                    }
                });

        CompletableFuture<EncaminhamentoResponse> futureEncaminhamento =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return encaminhamentoService.buscarHospitais(coIbge, TP_LEITO_CLINICO, MIN_HOSPITAIS);
                    } catch (Exception e) {
                        log.warn("[Busca] Encaminhamento falhou para {}: {}", coIbge, e.getMessage());
                        return EncaminhamentoResponse.builder()
                                .coIbge(coIbge)
                                .hospitais(List.of())
                                .build();
                    }
                });

        CompletableFuture.allOf(futurePerfil, futureRisco, futureEncaminhamento).join();

        PerfilEpidemiologicoResponse perfil = futurePerfil.join();
        PrevisaoRiscoResponse risco = futureRisco.join();
        EncaminhamentoResponse encaminhamento = futureEncaminhamento.join();

        // 5. Generate unified IA text
        String contextoUnificado = montarContextoUnificado(perfil, risco, encaminhamento);
        String textoIa;
        try {
            textoIa = iaService.gerarTextoOperacional(contextoUnificado);
        } catch (Exception e) {
            log.warn("[Busca] IA indisponível: {}", e.getMessage());
            textoIa = String.format(
                    "Em %d, %s registrou %d casos de %s (incidência: %.1f/100k hab.).",
                    perfil.getAno(), perfil.getMunicipio(),
                    perfil.getTotal(), perfil.getDoenca(),
                    perfil.getIncidencia());
        }

        return BuscaCompletaResponse.builder()
                .interpretacao(intencao)
                .perfil(perfil)
                .risco(risco)
                .encaminhamento(encaminhamento)
                .textoIa(textoIa)
                .build();
    }

    private Municipio encontrarMunicipio(String nome, String uf) {
        if (nome == null || nome.isBlank()) {
            throw new NotFoundException("Nome do município não identificado na pergunta");
        }
        if (uf != null && !uf.isBlank()) {
            List<Municipio> candidatos = municipioService.buscarPorNomeEUf(nome, uf);
            if (!candidatos.isEmpty()) {
                if (candidatos.size() > 1) {
                    log.info("[Busca] {} municípios encontrados para '{}'/{}. Usando o primeiro: {}",
                            candidatos.size(), nome, uf, candidatos.get(0).getNoMunicipio());
                }
                return candidatos.get(0);
            }
            // Try searching by name only
            return municipioService.buscarPorNome(nome)
                    .orElseThrow(() -> new NotFoundException(
                            "Município '" + nome + "' não encontrado. " +
                            "Tente usar o nome completo ou o código IBGE."));
        }
        throw new NotFoundException("UF não identificada na pergunta");
    }

    private String montarContextoUnificado(PerfilEpidemiologicoResponse perfil,
                                            PrevisaoRiscoResponse risco,
                                            EncaminhamentoResponse encaminhamento) {
        StringBuilder sb = new StringBuilder();

        if (perfil != null) {
            sb.append(String.format(
                    "Perfil epidemiológico: %s/%s, %d casos de %s em %d, " +
                    "incidência %.1f/100k hab, classificação %s.%n",
                    perfil.getMunicipio(), perfil.getUf(), perfil.getTotal(),
                    perfil.getDoenca(), perfil.getAno(),
                    perfil.getIncidencia(), perfil.getClassificacao()));
        }

        if (risco != null) {
            sb.append(String.format(
                    "Risco climático (próximas 2 semanas): score %d/8, " +
                    "classificação %s, fatores: %s.%n",
                    risco.getScore(), risco.getClassificacao(), risco.getFatores()));
        } else {
            sb.append("Risco climático: dados não disponíveis no momento.").append(System.lineSeparator());
        }

        if (encaminhamento != null && encaminhamento.getHospitais() != null
                && !encaminhamento.getHospitais().isEmpty()) {
            sb.append(String.format(
                    "Hospital de referência mais próximo: %s (%.1f km), %d leitos SUS, tel: %s.%n",
                    encaminhamento.getHospitais().get(0).getNoFantasia(),
                    encaminhamento.getHospitais().get(0).getDistanciaKm(),
                    encaminhamento.getHospitais().get(0).getQtLeitosSus(),
                    encaminhamento.getHospitais().get(0).getNuTelefone()));
        } else {
            sb.append("Hospitais de referência: nenhum encontrado com os critérios informados.")
              .append(System.lineSeparator());
        }

        return sb.toString();
    }
}
