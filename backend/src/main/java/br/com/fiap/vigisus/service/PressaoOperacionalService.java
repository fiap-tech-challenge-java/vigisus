package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PressaoOperacionalService {

    private final MunicipioService municipioService;
    private final CasoDengueRepository casoDengueRepository;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
    private final IaService iaService;

    public PressaoOperacionalResponse avaliarPressao(PressaoOperacionalRequest req) {
        String coIbge = req.getMunicipio();
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        // PASSO 1 — Contexto epidemiológico
        ContextoEpidemiologicoDTO contexto = construirContexto(coIbge);

        // PASSO 2 — Calcular nível de atenção
        PrevisaoRiscoResponse riscoClimatico = calcularRiscoComFallback(coIbge);
        String nivelAtencao = calcularNivelAtencao(req.getSuspeitasDengueDia(),
                contexto.getClassificacaoAtual(), contexto.getTendencia(), riscoClimatico);

        // PASSO 3 — Previsão próximos dias
        PrevisaoProximosDiasDTO previsao = construirPrevisao(riscoClimatico);

        // PASSO 4 — Campos informativos
        String contextoAtual = montarContextoAtual(req.getSuspeitasDengueDia(),
                municipio.getNoMunicipio(), contexto.getClassificacaoAtual(), contexto.getTendencia());
        String padraoHistorico = montarPadraoHistorico(contexto);
        List<String> checklistInformativo = montarChecklistInformativo(nivelAtencao,
                municipio.getNoMunicipio(), contexto, previsao);

        // PASSO 5 — Hospitais de referência (top 3)
        List<HospitalDTO> hospitaisReferencia = buscarHospitaisReferencia(coIbge);

        // PASSO 6 — Texto IA
        String contextoTexto = formatarContextoParaIa(contextoAtual, padraoHistorico, nivelAtencao);
        String textoIa = iaService.gerarTextoOperacional(contextoTexto);

        return PressaoOperacionalResponse.builder()
                .municipio(coIbge)
                .tipoUnidade(req.getTipoUnidade())
                .nivelAtencao(nivelAtencao)
                .contextoAtual(contextoAtual)
                .padraoHistorico(padraoHistorico)
                .checklistInformativo(checklistInformativo)
                .contexto(contexto)
                .previsao(previsao)
                .hospitaisReferencia(hospitaisReferencia)
                .textoIa(textoIa)
                .build();
    }

    // ── PASSO 1 ───────────────────────────────────────────────────────────────

    ContextoEpidemiologicoDTO construirContexto(String coIbge) {
        LocalDate today = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        int currentWeek = today.get(wf.weekOfWeekBasedYear());
        int currentYear = today.get(wf.weekBasedYear());

        // Last 4 week numbers (handling year boundary)
        List<Integer> currentYearWeeks = new ArrayList<>();
        List<Integer> prevYearWeeks = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            int w = currentWeek - i;
            if (w <= 0) {
                int lastWeekPrevYear = lastIsoWeekOfYear(currentYear - 1);
                prevYearWeeks.add(lastWeekPrevYear + w);
            } else {
                currentYearWeeks.add(w);
            }
        }

        // Query current year weeks
        Map<Integer, Long> casosSemana = new LinkedHashMap<>();
        if (!currentYearWeeks.isEmpty()) {
            List<Object[]> rows = casoDengueRepository.findCasosPorSemanas(coIbge, currentYear, currentYearWeeks);
            for (Object[] row : rows) {
                casosSemana.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        // Query previous year weeks if needed
        Map<Integer, Long> casosSemanaAnoAnterior = new LinkedHashMap<>();
        if (!prevYearWeeks.isEmpty()) {
            List<Object[]> rows = casoDengueRepository.findCasosPorSemanas(coIbge, currentYear - 1, prevYearWeeks);
            for (Object[] row : rows) {
                casosSemanaAnoAnterior.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        // Build combined ordered map of last 4 weeks (prev-year entries first)
        List<long[]> semanasOrdenadas = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            int w = currentWeek - i;
            if (w <= 0) {
                int lastWeekPrevYear = lastIsoWeekOfYear(currentYear - 1);
                int weekPrevYear = lastWeekPrevYear + w;
                semanasOrdenadas.add(new long[]{casosSemanaAnoAnterior.getOrDefault(weekPrevYear, 0L)});
            } else {
                semanasOrdenadas.add(new long[]{casosSemana.getOrDefault(w, 0L)});
            }
        }

        long casosSemanasAtual = semanasOrdenadas.get(3)[0];  // semana 0 (more recent)
        long casosSemana3Atras = semanasOrdenadas.get(0)[0];  // semana 3 weeks ago
        int casosUltimasSemanas = (int) semanasOrdenadas.stream()
                .mapToLong(a -> a[0]).sum();

        String tendencia = calcularTendencia(casosSemanasAtual, casosSemana3Atras);

        // Classificação anual
        String classificacaoAtual = classificarPorTotal(coIbge, currentYear);

        // Comparativo histórico: same weeks previous year
        Map<Integer, Long> casosMesmoPeriodoAnoAnterior = new LinkedHashMap<>();
        List<Object[]> rowsAnoAnterior = casoDengueRepository
                .findCasosPorSemanas(coIbge, currentYear - 1, currentYearWeeks.isEmpty()
                        ? prevYearWeeks : currentYearWeeks);
        for (Object[] row : rowsAnoAnterior) {
            casosMesmoPeriodoAnoAnterior.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        long totalAnoAnteriorMesmoPeriodo = casosMesmoPeriodoAnoAnterior.values()
                .stream().mapToLong(Long::longValue).sum();

        String comparativoHistorico = gerarComparativoHistorico(
                casosUltimasSemanas, totalAnoAnteriorMesmoPeriodo, currentYear);

        return ContextoEpidemiologicoDTO.builder()
                .classificacaoAtual(classificacaoAtual)
                .casosUltimasSemanas(casosUltimasSemanas)
                .tendencia(tendencia)
                .comparativoHistorico(comparativoHistorico)
                .build();
    }

    private String calcularTendencia(long casosAtual, long casosSemana3Atras) {
        if (casosSemana3Atras == 0) {
            return casosAtual > 0 ? "CRESCENTE" : "ESTAVEL";
        }
        if (casosAtual > casosSemana3Atras * 1.2) {
            return "CRESCENTE";
        } else if (casosAtual < casosSemana3Atras * 0.8) {
            return "DECRESCENTE";
        }
        return "ESTAVEL";
    }

    private String classificarPorTotal(String coIbge, int ano) {
        long total = casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(coIbge, ano);
        if (total == 0) {
            return "BAIXO";
        }
        try {
            Municipio m = municipioService.buscarPorCoIbge(coIbge);
            long populacao = m.getPopulacao() != null && m.getPopulacao() > 0 ? m.getPopulacao() : 1L;
            double incidencia = (double) total / populacao * 100_000;
            if (incidencia < 50) return "BAIXO";
            if (incidencia < 100) return "MODERADO";
            if (incidencia <= 300) return "ALTO";
            return "EPIDEMIA";
        } catch (Exception e) {
            return "BAIXO";
        }
    }

    private String gerarComparativoHistorico(int casosAtual, long casosAnoAnterior, int anoAtual) {
        if (casosAnoAnterior == 0) {
            return String.format("No mesmo período de %d não havia registros comparáveis.", anoAtual - 1);
        }
        long diff = casosAtual - casosAnoAnterior;
        if (diff > 0) {
            return String.format("Comparando com o mesmo período de %d: +%d casos a mais (+%.0f%%).",
                    anoAtual - 1, diff, (double) diff / casosAnoAnterior * 100);
        } else if (diff < 0) {
            return String.format("Comparando com o mesmo período de %d: %d casos a menos (%.0f%%).",
                    anoAtual - 1, diff, (double) diff / casosAnoAnterior * 100);
        }
        return String.format("Situação semelhante ao mesmo período de %d.", anoAtual - 1);
    }

    private int lastIsoWeekOfYear(int year) {
        LocalDate dec28 = LocalDate.of(year, 12, 28);
        return dec28.get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    // ── PASSO 2 ───────────────────────────────────────────────────────────────

    String calcularNivelAtencao(int suspeitasDia, String classificacao,
                                        String tendencia, PrevisaoRiscoResponse risco) {
        int score = 0;

        // Por suspeitas informadas hoje
        if (suspeitasDia >= 10) {
            score += 3;
        } else if (suspeitasDia >= 5) {
            score += 2;
        } else if (suspeitasDia >= 2) {
            score += 1;
        }

        // Por classificação epidemiológica
        switch (classificacao) {
            case "EPIDEMIA" -> score += 3;
            case "ALTO"     -> score += 2;
            case "MODERADO" -> score += 1;
            default -> { /* BAIXO = 0 */ }
        }

        // Por tendência
        if ("CRESCENTE".equals(tendencia)) {
            score += 2;
        }

        // Por risco climático
        if (risco != null) {
            switch (risco.getClassificacao()) {
                case "MUITO_ALTO" -> score += 2;
                case "ALTO"       -> score += 1;
                default -> { /* BAIXO/MODERADO = 0 */ }
            }
        }

        if (score <= 2) return "NORMAL";
        if (score <= 5) return "ELEVADO";
        return "CRITICO";
    }

    private PrevisaoRiscoResponse calcularRiscoComFallback(String coIbge) {
        try {
            return previsaoRiscoService.calcularRisco(coIbge);
        } catch (Exception e) {
            return null;
        }
    }

    // ── PASSO 3 ───────────────────────────────────────────────────────────────

    private PrevisaoProximosDiasDTO construirPrevisao(PrevisaoRiscoResponse risco) {
        if (risco == null) {
            return PrevisaoProximosDiasDTO.builder()
                    .riscoClimatico("Indisponível")
                    .tendencia7Dias("Previsão climática não disponível no momento.")
                    .build();
        }
        String riscoClimatico = String.format("Score %d/8 — %s", risco.getScore(), risco.getClassificacao());
        String tendencia7Dias = risco.getFatores() != null && !risco.getFatores().isEmpty()
                ? "Fatores de risco identificados: " + String.join("; ", risco.getFatores())
                : "Sem fatores de risco climático relevantes identificados.";
        return PrevisaoProximosDiasDTO.builder()
                .riscoClimatico(riscoClimatico)
                .tendencia7Dias(tendencia7Dias)
                .build();
    }

    // ── PASSO 4 — Campos informativos ────────────────────────────────────────

    private String montarContextoAtual(int suspeitasDia, String nomeMunicipio,
                                        String classificacao, String tendencia) {
        return String.format(
                "%d suspeitas registradas hoje em %s, município com classificação epidemiológica %s" +
                " e tendência %s nas últimas 4 semanas.",
                suspeitasDia, nomeMunicipio, classificacao, tendencia);
    }

    private String montarPadraoHistorico(ContextoEpidemiologicoDTO contexto) {
        String comparativo = contexto.getComparativoHistorico();
        if (comparativo == null || comparativo.contains("não havia registros")) {
            return "Dados históricos insuficientes para comparação do período.";
        }
        return comparativo;
    }

    private List<String> montarChecklistInformativo(String nivelAtencao, String nomeMunicipio,
                                                     ContextoEpidemiologicoDTO contexto,
                                                     PrevisaoProximosDiasDTO previsao) {
        return switch (nivelAtencao) {
            case "CRITICO" -> List.of(
                    "Volume de suspeitas compatível com situação de surto",
                    String.format("Contexto epidemiológico: %s — %d casos nas últimas 4 semanas",
                            contexto.getClassificacaoAtual(), contexto.getCasosUltimasSemanas()),
                    "Padrão climático favorável ao vetor nas próximas 2 semanas: " + previsao.getRiscoClimatico(),
                    "Hospitais de referência mais próximos disponíveis na lista abaixo",
                    "Contato Vigilância Epidemiológica Municipal: 0800-644-6645",
                    "Contato Central de Regulação MG: (31) 3916-6868");
            case "ELEVADO" -> List.of(
                    "Número de suspeitas acima da média para o período",
                    String.format("Contexto epidemiológico: %s — tendência %s",
                            contexto.getClassificacaoAtual(), contexto.getTendencia()),
                    "Risco climático nas próximas 2 semanas: " + previsao.getRiscoClimatico(),
                    "Hospitais de referência com leitos disponíveis listados abaixo");
            default -> List.of(
                    "Situação dentro do padrão histórico para o período",
                    "Capacidade hospitalar regional: verificar lista de referência",
                    "Previsão climática: " + previsao.getRiscoClimatico());
        };
    }

    // ── PASSO 5 — Hospitais de referência ────────────────────────────────────

    List<HospitalDTO> buscarHospitaisReferencia(String coIbge) {
        EncaminhamentoResponse clinicos = encaminhamentoService.buscarHospitais(coIbge, "74", 10);
        EncaminhamentoResponse uti = encaminhamentoService.buscarHospitais(coIbge, "81", 5);

        // Merge sem duplicatas, manter ordenação por distância
        Map<String, HospitalDTO> merged = new LinkedHashMap<>();
        if (clinicos.getHospitais() != null) {
            for (HospitalDTO h : clinicos.getHospitais()) {
                merged.put(h.getCoCnes(), h);
            }
        }
        if (uti.getHospitais() != null) {
            for (HospitalDTO h : uti.getHospitais()) {
                merged.putIfAbsent(h.getCoCnes(), h);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistanciaKm))
                .limit(3)
                .collect(Collectors.toList());
    }

    // ── PASSO 6 — Formato do prompt para IA ──────────────────────────────────

    private String formatarContextoParaIa(String contextoAtual, String padraoHistorico, String nivelAtencao) {
        return String.format("Dados: %s | %s | Nível de atenção: %s",
                contextoAtual, padraoHistorico, nivelAtencao);
    }
}
