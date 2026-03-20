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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/busca")
@Tag(name = "Busca por Linguagem Natural")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuscaController {

    private static final String TP_LEITO_CLINICO = "74"; // CNES: leito clínico

    private final IaService iaService;
    private final PerfilEpidemiologicoService perfilService;
    private final MunicipioService municipioService;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
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

        // 2. Find municipality by name within UF
        Municipio municipio = encontrarMunicipio(intencao.getMunicipio(), intencao.getUf());
        String coIbge = municipio.getCoIbge();
        intencao.setCoIbge(coIbge);

        // 3. In parallel: perfil, risco, encaminhamento
        int ano = intencao.getAno() != null ? intencao.getAno() : LocalDate.now().getYear();
        String doenca = intencao.getDoenca() != null ? intencao.getDoenca() : "dengue";

        CompletableFuture<PerfilEpidemiologicoResponse> perfilFuture = CompletableFuture
                .supplyAsync(() -> perfilService.gerarPerfil(coIbge, doenca, ano));

        CompletableFuture<PrevisaoRiscoResponse> riscoFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return previsaoRiscoService.calcularRisco(coIbge);
                    } catch (Exception e) {
                        return null;
                    }
                });

        CompletableFuture<EncaminhamentoResponse> encaminhamentoFuture = CompletableFuture
                .supplyAsync(() -> encaminhamentoService.buscarHospitais(coIbge, TP_LEITO_CLINICO, 5));

        CompletableFuture.allOf(perfilFuture, riscoFuture, encaminhamentoFuture).join();

        PerfilEpidemiologicoResponse perfil = perfilFuture.join();
        PrevisaoRiscoResponse risco = riscoFuture.join();
        EncaminhamentoResponse encaminhamento = encaminhamentoFuture.join();

        // 4. Generate unified IA narrative
        String contextoUnificado = montarContextoUnificado(perfil, risco, encaminhamento);
        String textoIa = iaService.gerarTextoBuscaCompleta(contextoUnificado);

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
            List<Municipio> candidatos = municipioService.listarPorUf(uf);
            return candidatos.stream()
                    .filter(m -> m.getNoMunicipio().equalsIgnoreCase(nome))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(
                            "Município '" + nome + "' não encontrado. Tente informar o código IBGE diretamente."));
        }
        throw new NotFoundException("UF não identificada na pergunta");
    }

    private String montarContextoUnificado(PerfilEpidemiologicoResponse perfil,
                                            PrevisaoRiscoResponse risco,
                                            EncaminhamentoResponse encaminhamento) {
        StringBuilder sb = new StringBuilder();
        if (perfil != null) {
            sb.append(String.format("Perfil: %s/%s, %d casos de %s em %d, incidência %.1f/100k, classificação %s. ",
                    perfil.getMunicipio(), perfil.getUf(),
                    perfil.getTotal(), perfil.getDoenca(), perfil.getAno(),
                    perfil.getIncidencia(), perfil.getClassificacao()));
        }
        if (risco != null) {
            sb.append(String.format("Risco climático: score %d/8 — %s. Fatores: %s. ",
                    risco.getScore(), risco.getClassificacao(), risco.getFatores()));
        }
        if (encaminhamento != null && encaminhamento.getHospitais() != null
                && !encaminhamento.getHospitais().isEmpty()) {
            sb.append("Hospitais de referência: ");
            encaminhamento.getHospitais().stream().limit(3).forEach(h ->
                    sb.append(String.format("%s (%.1f km, %d leitos SUS), ",
                            h.getNoFantasia(), h.getDistanciaKm(), h.getQtLeitosSus())));
        }
        return sb.toString().trim();
    }
}
