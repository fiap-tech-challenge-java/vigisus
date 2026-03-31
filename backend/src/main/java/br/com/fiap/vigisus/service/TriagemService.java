package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.domain.triagem.CalculadoraScoreTriagem;
import br.com.fiap.vigisus.domain.triagem.PriorizacaoTriagemPolicy;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import br.com.fiap.vigisus.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/**
 * Camada de orquestração que coordena use cases e integrações externas.
 *
 * <p>Depende exclusivamente de Ports (application/port/) — nunca de repositórios
 * JPA diretamente — respeitando a regra de dependência da Clean Architecture.
 *
 * <p>Candidato à migração para use case dedicado na versão 2.0.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriagemService {

    private static final List<String> SINAIS_ALARME = List.of(
            "Dor abdominal intensa",
            "V\u00f4mitos persistentes",
            "Sangramento espont\u00e2neo",
            "Prostra\u00e7\u00e3o extrema",
            "Queda abrupta de temperatura",
            "Hipotens\u00e3o",
            "Dificuldade respirat\u00f3ria"
    );

    private final PerfilEpidemiologicoService perfilService;
    private final EncaminhamentoService encaminhamentoService;
    private final IaService iaService;
    private final CalculadoraScoreTriagem calculadoraScoreTriagem;
    private final PriorizacaoTriagemPolicy priorizacaoTriagemPolicy;
    private final AdminMetricsService adminMetricsService;

    public TriagemResponse avaliar(TriagemRequest req) {
        adminMetricsService.registrarTriagem();
        double score = calcularScore(req);

        int anoAtual = Year.now().getValue();
        PerfilEpidemiologicoResponse perfil = buscarPerfilComFallback(req.getMunicipio(), anoAtual);
        String classificacao = perfil != null ? perfil.getClassificacao() : "MODERADO";
        double multiplicador = resolverMultiplicador(classificacao);
        double scoreFinal = score * multiplicador;

        String prioridade = classificarPrioridade(scoreFinal);
        String corProtocolo = resolverCor(prioridade);

        String alertaEpidemiologico = montarAlerta(perfil, req.getMunicipio(), classificacao);
        String recomendacao = montarRecomendacao(prioridade);
        boolean requerObservacao = priorizacaoTriagemPolicy.requerObservacao(prioridade);

        EncaminhamentoResponse.HospitalDTO encaminhamento = null;
        if (requerObservacao) {
            encaminhamento = buscarPrimeiroHospital(req.getMunicipio());
        }

        String textoIa = gerarTextoIaComFallback(prioridade, req.getSintomas(), alertaEpidemiologico);

        return TriagemResponse.builder()
                .prioridade(prioridade)
                .corProtocolo(corProtocolo)
                .alertaEpidemiologico(alertaEpidemiologico)
                .recomendacao(recomendacao)
                .sinaisAlarme(SINAIS_ALARME)
                .requerObservacao(requerObservacao)
                .encaminhamento(encaminhamento)
                .textoIa(textoIa)
                .build();
    }

    double calcularScore(TriagemRequest req) {
        return calculadoraScoreTriagem.calcular(req);
    }

    private PerfilEpidemiologicoResponse buscarPerfilComFallback(String municipio, int ano) {
        try {
            return perfilService.gerarPerfil(municipio, "dengue", ano);
        } catch (Exception e) {
            log.warn("N\u00e3o foi poss\u00edvel obter perfil epidemiol\u00f3gico para {}: {}", municipio, e.getMessage());
            return null;
        }
    }

    double resolverMultiplicador(String classificacao) {
        return priorizacaoTriagemPolicy.resolverMultiplicador(classificacao);
    }

    String classificarPrioridade(double scoreFinal) {
        return priorizacaoTriagemPolicy.classificarPrioridade(scoreFinal);
    }

    private String resolverCor(String prioridade) {
        return priorizacaoTriagemPolicy.resolverCor(prioridade);
    }

    private String montarAlerta(PerfilEpidemiologicoResponse perfil, String municipioCode, String classificacao) {
        if (perfil == null) {
            return String.format("Situa\u00e7\u00e3o epidemiol\u00f3gica %s em %s.", classificacao, municipioCode);
        }

        String nomeMunicipio = perfil.getMunicipio();

        if ("EPIDEMIA".equals(classificacao)) {
            String posicao = perfil.getComparativoEstado() != null
                    ? perfil.getComparativoEstado().getPosicaoRankingEstado()
                    : null;
            String posicaoTexto = posicao != null
                    ? extrairPrimeiraPosicao(posicao) + "\u00ba munic\u00edpio mais afetado em " + perfil.getUf()
                    : "munic\u00edpio com alta incid\u00eancia em " + perfil.getUf();
            return String.format(
                    "%s est\u00e1 em situa\u00e7\u00e3o de EPIDEMIA de dengue. " +
                            "%d casos notificados em %d. Incid\u00eancia de %.1f/100 mil hab \u2014 %s.",
                    nomeMunicipio,
                    perfil.getTotal(),
                    perfil.getAno(),
                    perfil.getIncidencia(),
                    posicaoTexto
            );
        }

        if ("ALTO".equals(classificacao)) {
            return String.format(
                    "Situa\u00e7\u00e3o de ALERTA em %s. %d casos em %d, acima da m\u00e9dia hist\u00f3rica para o per\u00edodo.",
                    nomeMunicipio,
                    perfil.getTotal(),
                    perfil.getAno()
            );
        }

        return String.format("Situa\u00e7\u00e3o epidemiol\u00f3gica %s em %s.", classificacao, nomeMunicipio);
    }

    private String extrairPrimeiraPosicao(String posicaoRankingEstado) {
        if (posicaoRankingEstado == null) {
            return "N/A";
        }
        int idx = posicaoRankingEstado.indexOf(' ');
        return idx > 0 ? posicaoRankingEstado.substring(0, idx) : posicaoRankingEstado;
    }

    private String montarRecomendacao(String prioridade) {
        return switch (prioridade) {
            case "CRITICA" ->
                    "Perfil cl\u00ednico cr\u00edtico + contexto epidemiol\u00f3gico de alto risco. " +
                            "Solicitar NS1, hemograma e plaquetas IMEDIATAMENTE. " +
                            "Acesso venoso. Monitoriza\u00e7\u00e3o cont\u00ednua. Acionar m\u00e9dico.";
            case "ALTA" ->
                    "Suspeita forte de dengue com sinais de alarme. " +
                            "Solicitar NS1 e hemograma. Manter em observa\u00e7\u00e3o por m\u00ednimo 4 horas. " +
                            "Hidrata\u00e7\u00e3o venosa se indicado.";
            case "MEDIA" ->
                    "Suspeita de dengue em fase febril. Solicitar NS1. " +
                            "Orientar hidrata\u00e7\u00e3o oral. Retornar se sinais de alarme.";
            default ->
                    "Sintomas inespec\u00edficos em contexto epidemiol\u00f3gico controlado. " +
                            "Orientar cuidados em domic\u00edlio. Retornar se persist\u00eancia ou piora em 48h.";
        };
    }

    private EncaminhamentoResponse.HospitalDTO buscarPrimeiroHospital(String municipio) {
        try {
            EncaminhamentoResponse resp = encaminhamentoService.buscarHospitais(municipio, "81", 3);
            if (resp.getHospitais() != null && !resp.getHospitais().isEmpty()) {
                return resp.getHospitais().get(0);
            }
        } catch (Exception e) {
            log.warn("N\u00e3o foi poss\u00edvel buscar hospitais para {}: {}", municipio, e.getMessage());
        }
        return null;
    }

    private String gerarTextoIaComFallback(String prioridade, List<String> sintomas, String alertaEpidemiologico) {
        try {
            return iaService.gerarTextoTriagem(prioridade, sintomas, alertaEpidemiologico);
        } catch (Exception e) {
            log.warn("IA indispon\u00edvel para triagem: {}", e.getMessage());
            return null;
        }
    }
}
