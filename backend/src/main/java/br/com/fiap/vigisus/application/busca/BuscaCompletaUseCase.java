package br.com.fiap.vigisus.application.busca;

import br.com.fiap.vigisus.dto.BuscaCompletaResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import br.com.fiap.vigisus.exception.NotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.service.EncaminhamentoService;
import br.com.fiap.vigisus.service.IaService;
import br.com.fiap.vigisus.service.MunicipioService;
import br.com.fiap.vigisus.service.PerfilEpidemiologicoService;
import br.com.fiap.vigisus.service.PrevisaoRiscoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscaCompletaUseCase {

    private static final String TP_LEITO_CLINICO = "74";
    private static final int MIN_HOSPITAIS = 5;

    private final IaService iaService;
    private final PerfilEpidemiologicoService perfilService;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
    private final MunicipioService municipioService;

    public BuscaCompletaResponse buscarPorPergunta(String pergunta) {
        IntencaoDTO intencao = iaService.interpretarPergunta(pergunta);
        Municipio municipio = encontrarMunicipio(intencao.getMunicipio(), intencao.getUf());
        String coIbge = municipio.getCoIbge();
        intencao.setCoIbge(coIbge);

        BuscaCompletaResponse response = buscarPorCoIbge(
                coIbge,
                resolverDoenca(intencao.getDoenca()),
                resolverAno(intencao.getAno())
        );
        response.setInterpretacao(intencao);
        return response;
    }

    public BuscaCompletaResponse buscarDireto(String municipio, String uf, String doenca, Integer ano) {
        Municipio municipioEncontrado = municipioService.buscarPorNomeEUf(municipio.trim(), uf.trim())
                .stream()
                .findFirst()
                .orElseThrow(() -> new MunicipioNotFoundException(municipio.trim() + " / " + uf.trim()));

        return buscarPorCoIbge(
                municipioEncontrado.getCoIbge(),
                resolverDoenca(doenca),
                resolverAno(ano)
        );
    }

    private BuscaCompletaResponse buscarPorCoIbge(String coIbge, String doenca, int ano) {
        CompletableFuture<PerfilEpidemiologicoResponse> futurePerfil =
                CompletableFuture.supplyAsync(() -> perfilService.gerarPerfil(coIbge, doenca, ano));

        CompletableFuture<PrevisaoRiscoResponse> futureRisco =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return previsaoRiscoService.calcularRisco(coIbge);
                    } catch (Exception e) {
                        log.warn("[Busca] Risco indisponivel para {}: {}", coIbge, e.getMessage());
                        return PrevisaoRiscoResponse.builder()
                                .coIbge(coIbge)
                                .score(0)
                                .classificacao("INDISPONIVEL")
                                .fatores(List.of("Previsao climatica temporariamente indisponivel"))
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

        return BuscaCompletaResponse.builder()
                .perfil(perfil)
                .risco(risco)
                .encaminhamento(encaminhamento)
                .textoIa(gerarTextoOperacional(perfil, risco, encaminhamento))
                .build();
    }

    private String gerarTextoOperacional(
            PerfilEpidemiologicoResponse perfil,
            PrevisaoRiscoResponse risco,
            EncaminhamentoResponse encaminhamento
    ) {
        String contextoUnificado = montarContextoUnificado(perfil, risco, encaminhamento);
        try {
            return iaService.gerarTextoOperacional(contextoUnificado);
        } catch (Exception e) {
            log.warn("[Busca] IA indisponivel: {}", e.getMessage());
            return String.format(
                    "Em %d, %s registrou %d casos de %s (incidencia: %.1f/100k hab.).",
                    perfil.getAno(),
                    perfil.getMunicipio(),
                    perfil.getTotal(),
                    perfil.getDoenca(),
                    perfil.getIncidencia()
            );
        }
    }

    private Municipio encontrarMunicipio(String nome, String uf) {
        if (nome == null || nome.isBlank()) {
            throw new NotFoundException("Nome do municipio nao identificado na pergunta");
        }

        if (uf == null || uf.isBlank()) {
            throw new NotFoundException("UF nao identificada na pergunta");
        }

        List<Municipio> candidatos = municipioService.buscarPorNomeEUf(nome, uf);
        if (!candidatos.isEmpty()) {
            if (candidatos.size() > 1) {
                log.info("[Busca] {} municipios encontrados para '{}'/{}. Usando o primeiro: {}",
                        candidatos.size(), nome, uf, candidatos.get(0).getNoMunicipio());
            }
            return candidatos.get(0);
        }

        return municipioService.buscarPorNome(nome)
                .orElseThrow(() -> new NotFoundException(
                        "Municipio '" + nome + "' nao encontrado. " +
                                "Tente usar o nome completo ou o codigo IBGE."
                ));
    }

    private int resolverAno(Integer ano) {
        return ano != null ? ano : LocalDate.now().getYear();
    }

    private String resolverDoenca(String doenca) {
        return doenca != null && !doenca.isBlank() ? doenca : "dengue";
    }

    private String montarContextoUnificado(
            PerfilEpidemiologicoResponse perfil,
            PrevisaoRiscoResponse risco,
            EncaminhamentoResponse encaminhamento
    ) {
        StringBuilder contexto = new StringBuilder();

        if (perfil != null) {
            contexto.append(String.format(
                    "Perfil epidemiologico: %s/%s, %d casos de %s em %d, incidencia %.1f/100k hab, classificacao %s.%n",
                    perfil.getMunicipio(),
                    perfil.getUf(),
                    perfil.getTotal(),
                    perfil.getDoenca(),
                    perfil.getAno(),
                    perfil.getIncidencia(),
                    perfil.getClassificacao()
            ));
        }

        if (risco != null) {
            contexto.append(String.format(
                    "Risco climatico (proximas 2 semanas): score %d/8, classificacao %s, fatores: %s.%n",
                    risco.getScore(),
                    risco.getClassificacao(),
                    risco.getFatores()
            ));
        } else {
            contexto.append("Risco climatico: dados nao disponiveis no momento.")
                    .append(System.lineSeparator());
        }

        if (encaminhamento != null
                && encaminhamento.getHospitais() != null
                && !encaminhamento.getHospitais().isEmpty()) {
            EncaminhamentoResponse.HospitalDTO hospital = encaminhamento.getHospitais().get(0);
            contexto.append(String.format(
                    "Hospital de referencia mais proximo: %s (%.1f km), %d leitos SUS, tel: %s.%n",
                    hospital.getNoFantasia(),
                    hospital.getDistanciaKm(),
                    hospital.getQtLeitosSus(),
                    hospital.getNuTelefone()
            ));
        } else {
            contexto.append("Hospitais de referencia: nenhum encontrado com os criterios informados.")
                    .append(System.lineSeparator());
        }

        return contexto.toString();
    }
}
