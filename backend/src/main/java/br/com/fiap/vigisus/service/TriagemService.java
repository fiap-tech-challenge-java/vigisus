package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.TriagemRequest;
import br.com.fiap.vigisus.dto.TriagemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriagemService {

    private static final List<String> SINAIS_ALARME = List.of(
            "Dor abdominal intensa",
            "Vômitos persistentes",
            "Sangramento espontâneo",
            "Prostração extrema",
            "Queda abrupta de temperatura",
            "Hipotensão",
            "Dificuldade respiratória"
    );

    private final PerfilEpidemiologicoService perfilService;
    private final EncaminhamentoService encaminhamentoService;
    private final IaService iaService;

    public TriagemResponse avaliar(TriagemRequest req) {
        // PASSO 1 — Calcular score de sintomas
        double score = calcularScore(req);

        // PASSO 2 — Buscar contexto epidemiológico e aplicar multiplicador
        int anoAtual = Year.now().getValue();
        PerfilEpidemiologicoResponse perfil = buscarPerfilComFallback(req.getMunicipio(), anoAtual);
        String classificacao = perfil != null ? perfil.getClassificacao() : "MODERADO";
        double multiplicador = resolverMultiplicador(classificacao);
        double scoreFinal = score * multiplicador;

        // PASSO 3 — Classificar prioridade final
        String prioridade = classificarPrioridade(scoreFinal);
        String corProtocolo = resolverCor(prioridade);

        // PASSO 4 — Montar alertaEpidemiologico
        String alertaEpidemiologico = montarAlerta(perfil, req.getMunicipio(), classificacao);

        // PASSO 5 — Montar recomendacao
        String recomendacao = montarRecomendacao(prioridade);

        // PASSO 6 — Definir sinaisAlarme
        List<String> sinaisAlarme = SINAIS_ALARME;

        // PASSO 7 — Encaminhamento se ALTA ou CRITICA
        boolean requerObservacao = "ALTA".equals(prioridade) || "CRITICA".equals(prioridade);
        EncaminhamentoResponse.HospitalDTO encaminhamento = null;
        if (requerObservacao) {
            encaminhamento = buscarPrimeiroHospital(req.getMunicipio());
        }

        // PASSO 8 — Texto IA
        String textoIa = gerarTextoIaComFallback(prioridade, req.getSintomas(), alertaEpidemiologico);

        return TriagemResponse.builder()
                .prioridade(prioridade)
                .corProtocolo(corProtocolo)
                .alertaEpidemiologico(alertaEpidemiologico)
                .recomendacao(recomendacao)
                .sinaisAlarme(sinaisAlarme)
                .requerObservacao(requerObservacao)
                .encaminhamento(encaminhamento)
                .textoIa(textoIa)
                .build();
    }

    double calcularScore(TriagemRequest req) {
        List<String> sintomas = req.getSintomas() != null ? req.getSintomas() : List.of();
        List<String> comorbidades = req.getComorbidades() != null ? req.getComorbidades() : List.of();

        double score = 0;

        // Sintomas de alarme (+3 cada)
        for (String s : sintomas) {
            if (List.of("dor_abdominal", "sangramento", "vomito", "prostacao", "falta_ar").contains(s)) {
                score += 3;
            }
        }

        // Sintomas clássicos de dengue (+1 cada)
        for (String s : sintomas) {
            if (List.of("febre", "dor_muscular", "dor_retro_orbital", "cefaleia",
                    "exantema", "nausea", "tontura").contains(s)) {
                score += 1;
            }
        }

        // Comorbidades de risco (+2 cada)
        for (String c : comorbidades) {
            if (List.of("diabetes", "gestante", "doenca_renal", "doenca_cardiaca",
                    "imunossupressao").contains(c)) {
                score += 2;
            }
        }

        // Comorbidades moderadas (+1 cada)
        for (String c : comorbidades) {
            if (List.of("hipertensao", "obesidade").contains(c)) {
                score += 1;
            }
        }

        // Idade extrema
        if (req.getIdade() < 2 || req.getIdade() > 65) {
            score += 2;
        }

        // Dias de sintomas >= 5 → fase crítica
        if (req.getDiasSintomas() >= 5) {
            score += 1;
        }

        return score;
    }

    private PerfilEpidemiologicoResponse buscarPerfilComFallback(String municipio, int ano) {
        try {
            return perfilService.gerarPerfil(municipio, "dengue", ano);
        } catch (Exception e) {
            log.warn("Não foi possível obter perfil epidemiológico para {}: {}", municipio, e.getMessage());
            return null;
        }
    }

    double resolverMultiplicador(String classificacao) {
        if (classificacao == null) {
            return 1.0;
        }
        return switch (classificacao) {
            case "EPIDEMIA" -> 1.5;
            case "ALTO"     -> 1.2;
            case "BAIXO"    -> 0.8;
            default         -> 1.0;
        };
    }

    String classificarPrioridade(double scoreFinal) {
        if (scoreFinal < 3) return "BAIXA";
        if (scoreFinal <= 5) return "MEDIA";
        if (scoreFinal <= 8) return "ALTA";
        return "CRITICA";
    }

    private String resolverCor(String prioridade) {
        return switch (prioridade) {
            case "BAIXA"   -> "VERDE";
            case "MEDIA"   -> "AMARELO";
            case "ALTA"    -> "LARANJA";
            default        -> "VERMELHO";
        };
    }

    private String montarAlerta(PerfilEpidemiologicoResponse perfil, String municipioCode,
                                 String classificacao) {
        if (perfil == null) {
            return String.format("Situação epidemiológica %s em %s.", classificacao, municipioCode);
        }

        String nomeMunicipio = perfil.getMunicipio();

        if ("EPIDEMIA".equals(classificacao)) {
            String posicao = perfil.getComparativoEstado() != null
                    ? perfil.getComparativoEstado().getPosicaoRankingEstado()
                    : null;
            String posicaoTexto = posicao != null
                    ? extrairPrimeiraPosicao(posicao) + "º município mais afetado em " + perfil.getUf()
                    : "município com alta incidência em " + perfil.getUf();
            return String.format(
                    "%s está em situação de EPIDEMIA de dengue. " +
                    "%d casos notificados em %d. Incidência de %.1f/100 mil hab — %s.",
                    nomeMunicipio,
                    perfil.getTotal(),
                    perfil.getAno(),
                    perfil.getIncidencia(),
                    posicaoTexto);
        }

        if ("ALTO".equals(classificacao)) {
            return String.format(
                    "Situação de ALERTA em %s. %d casos em %d, acima da média histórica para o período.",
                    nomeMunicipio, perfil.getTotal(), perfil.getAno());
        }

        return String.format("Situação epidemiológica %s em %s.", classificacao, nomeMunicipio);
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
                    "Perfil clínico crítico + contexto epidemiológico de alto risco. " +
                    "Solicitar NS1, hemograma e plaquetas IMEDIATAMENTE. " +
                    "Acesso venoso. Monitorização contínua. Acionar médico.";
            case "ALTA" ->
                    "Suspeita forte de dengue com sinais de alarme. " +
                    "Solicitar NS1 e hemograma. Manter em observação por mínimo 4 horas. " +
                    "Hidratação venosa se indicado.";
            case "MEDIA" ->
                    "Suspeita de dengue em fase febril. Solicitar NS1. " +
                    "Orientar hidratação oral. Retornar se sinais de alarme.";
            default ->
                    "Sintomas inespecíficos em contexto epidemiológico controlado. " +
                    "Orientar cuidados em domicílio. Retornar se persistência ou piora em 48h.";
        };
    }

    private EncaminhamentoResponse.HospitalDTO buscarPrimeiroHospital(String municipio) {
        try {
            EncaminhamentoResponse resp = encaminhamentoService.buscarHospitais(municipio, "81", 3);
            if (resp.getHospitais() != null && !resp.getHospitais().isEmpty()) {
                return resp.getHospitais().get(0);
            }
        } catch (Exception e) {
            log.warn("Não foi possível buscar hospitais para {}: {}", municipio, e.getMessage());
        }
        return null;
    }

    private String gerarTextoIaComFallback(String prioridade, List<String> sintomas,
                                            String alertaEpidemiologico) {
        try {
            return iaService.gerarTextoTriagem(prioridade, sintomas, alertaEpidemiologico);
        } catch (Exception e) {
            log.warn("IA indisponível para triagem: {}", e.getMessage());
            return null;
        }
    }
}
