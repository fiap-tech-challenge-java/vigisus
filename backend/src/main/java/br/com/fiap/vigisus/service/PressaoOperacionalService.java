package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.operacional.MescladorHospitaisReferencia;
import br.com.fiap.vigisus.application.operacional.MontadorContextoOperacional;
import br.com.fiap.vigisus.application.operacional.MontadorPrevisaoOperacional;
import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.domain.operacional.CalculadoraNivelAtencaoOperacional;
import br.com.fiap.vigisus.domain.operacional.CalculadoraTendenciaOperacional;
import br.com.fiap.vigisus.domain.operacional.ChecklistOperacionalPolicy;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalRequest;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.PrevisaoProximosDiasDTO;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PressaoOperacionalService {

    private final MunicipioService municipioService;
    private final CasoDenguePort casoDenguePort;
    private final PrevisaoRiscoService previsaoRiscoService;
    private final EncaminhamentoService encaminhamentoService;
    private final IaService iaService;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;
    private final CalculadoraTendenciaOperacional calculadoraTendenciaOperacional;
    private final CalculadoraNivelAtencaoOperacional calculadoraNivelAtencaoOperacional;
    private final MontadorPrevisaoOperacional montadorPrevisaoOperacional;
    private final MontadorContextoOperacional montadorContextoOperacional;
    private final ChecklistOperacionalPolicy checklistOperacionalPolicy;
    private final MescladorHospitaisReferencia mescladorHospitaisReferencia;

    public PressaoOperacionalResponse avaliarPressao(PressaoOperacionalRequest req) {
        String coIbge = req.getMunicipio();
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        ContextoEpidemiologicoDTO contexto = construirContexto(coIbge);

        PrevisaoRiscoResponse riscoClimatico = calcularRiscoComFallback(coIbge);
        String nivelAtencao = calcularNivelAtencao(
                req.getSuspeitasDengueDia(),
                contexto.getClassificacaoAtual(),
                contexto.getTendencia(),
                riscoClimatico
        );

        PrevisaoProximosDiasDTO previsao = construirPrevisao(riscoClimatico);

        String contextoAtual = montarContextoAtual(
                req.getSuspeitasDengueDia(),
                municipio.getNoMunicipio(),
                contexto.getClassificacaoAtual(),
                contexto.getTendencia()
        );
        String padraoHistorico = montarPadraoHistorico(contexto.getComparativoHistorico());

        List<String> checklistInformativo = montarChecklistInformativo(nivelAtencao, contexto, previsao);
        List<HospitalDTO> hospitaisReferencia = buscarHospitaisReferencia(coIbge);

        String contextoTexto = montadorContextoOperacional.montarContextoIa(
                contextoAtual,
                padraoHistorico,
                nivelAtencao
        );
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

    ContextoEpidemiologicoDTO construirContexto(String coIbge) {
        LocalDate today = LocalDate.now();
        WeekFields wf = WeekFields.ISO;
        int currentWeek = today.get(wf.weekOfWeekBasedYear());
        int currentYear = today.get(wf.weekBasedYear());

        List<Integer> currentYearWeeks = new ArrayList<>();
        List<Integer> prevYearWeeks = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            int week = currentWeek - i;
            if (week <= 0) {
                int lastWeekPrevYear = lastIsoWeekOfYear(currentYear - 1);
                prevYearWeeks.add(lastWeekPrevYear + week);
            } else {
                currentYearWeeks.add(week);
            }
        }

        Map<Integer, Long> casosSemana = new LinkedHashMap<>();
        if (!currentYearWeeks.isEmpty()) {
            List<Object[]> rows = casoDenguePort.findCasosPorSemanas(coIbge, currentYear, currentYearWeeks);
            for (Object[] row : rows) {
                casosSemana.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        Map<Integer, Long> casosSemanaAnoAnterior = new LinkedHashMap<>();
        if (!prevYearWeeks.isEmpty()) {
            List<Object[]> rows = casoDenguePort.findCasosPorSemanas(coIbge, currentYear - 1, prevYearWeeks);
            for (Object[] row : rows) {
                casosSemanaAnoAnterior.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        List<long[]> semanasOrdenadas = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            int week = currentWeek - i;
            if (week <= 0) {
                int lastWeekPrevYear = lastIsoWeekOfYear(currentYear - 1);
                int weekPrevYear = lastWeekPrevYear + week;
                semanasOrdenadas.add(new long[]{casosSemanaAnoAnterior.getOrDefault(weekPrevYear, 0L)});
            } else {
                semanasOrdenadas.add(new long[]{casosSemana.getOrDefault(week, 0L)});
            }
        }

        long casosSemanasAtual = semanasOrdenadas.get(3)[0];
        long casosSemana3Atras = semanasOrdenadas.get(0)[0];
        int casosUltimasSemanas = (int) semanasOrdenadas.stream().mapToLong(a -> a[0]).sum();

        String tendencia = calcularTendencia(casosSemanasAtual, casosSemana3Atras);
        String classificacaoAtual = classificarPorTotal(coIbge, currentYear);

        Map<Integer, Long> casosMesmoPeriodoAnoAnterior = new LinkedHashMap<>();
        List<Object[]> rowsAnoAnterior = casoDenguePort.findCasosPorSemanas(
                coIbge,
                currentYear - 1,
                currentYearWeeks.isEmpty() ? prevYearWeeks : currentYearWeeks
        );
        for (Object[] row : rowsAnoAnterior) {
            casosMesmoPeriodoAnoAnterior.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        long totalAnoAnteriorMesmoPeriodo = casosMesmoPeriodoAnoAnterior.values().stream().mapToLong(Long::longValue).sum();

        String comparativoHistorico = gerarComparativoHistorico(
                casosUltimasSemanas,
                totalAnoAnteriorMesmoPeriodo,
                currentYear
        );

        return ContextoEpidemiologicoDTO.builder()
                .classificacaoAtual(classificacaoAtual)
                .casosUltimasSemanas(casosUltimasSemanas)
                .tendencia(tendencia)
                .comparativoHistorico(comparativoHistorico)
                .build();
    }

    String calcularNivelAtencao(int suspeitasDia, String classificacao, String tendencia, PrevisaoRiscoResponse risco) {
        return calculadoraNivelAtencaoOperacional.calcular(
                suspeitasDia,
                classificacao,
                tendencia,
                risco != null ? risco.getClassificacao() : null
        );
    }

    List<HospitalDTO> buscarHospitaisReferencia(String coIbge) {
        EncaminhamentoResponse clinicos = encaminhamentoService.buscarHospitais(coIbge, "74", 10);
        EncaminhamentoResponse uti = encaminhamentoService.buscarHospitais(coIbge, "81", 5);
        return mescladorHospitaisReferencia.mesclar(clinicos.getHospitais(), uti.getHospitais(), 3);
    }

    private String calcularTendencia(long casosAtual, long casosSemana3Atras) {
        return calculadoraTendenciaOperacional.calcular(casosAtual, casosSemana3Atras);
    }

    private String classificarPorTotal(String coIbge, int ano) {
        long total = casoDenguePort.sumTotalCasosByCoMunicipioAndAno(coIbge, ano);
        if (total == 0) {
            return "BAIXO";
        }
        try {
            Municipio municipio = municipioService.buscarPorCoIbge(coIbge);
            long populacao = municipio.getPopulacao() != null && municipio.getPopulacao() > 0 ? municipio.getPopulacao() : 1L;
            double incidencia = (double) total / populacao * 100_000;
            return classificacaoEpidemiologicaPolicy.classificar(incidencia);
        } catch (Exception e) {
            return "BAIXO";
        }
    }

    private String gerarComparativoHistorico(int casosAtual, long casosAnoAnterior, int anoAtual) {
        if (casosAnoAnterior == 0) {
            return String.format("No mesmo perÃƒÂ­odo de %d nÃƒÂ£o havia registros comparÃƒÂ¡veis.", anoAtual - 1);
        }
        long diff = casosAtual - casosAnoAnterior;
        if (diff > 0) {
            return String.format(
                    "Comparando com o mesmo perÃƒÂ­odo de %d: +%d casos a mais (+%.0f%%).",
                    anoAtual - 1,
                    diff,
                    (double) diff / casosAnoAnterior * 100
            );
        }
        if (diff < 0) {
            return String.format(
                    "Comparando com o mesmo perÃƒÂ­odo de %d: %d casos a menos (%.0f%%).",
                    anoAtual - 1,
                    diff,
                    (double) diff / casosAnoAnterior * 100
            );
        }
        return String.format("SituaÃƒÂ§ÃƒÂ£o semelhante ao mesmo perÃƒÂ­odo de %d.", anoAtual - 1);
    }

    private int lastIsoWeekOfYear(int year) {
        LocalDate dec28 = LocalDate.of(year, 12, 28);
        return dec28.get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    private PrevisaoRiscoResponse calcularRiscoComFallback(String coIbge) {
        try {
            return previsaoRiscoService.calcularRisco(coIbge);
        } catch (Exception e) {
            return null;
        }
    }

    private PrevisaoProximosDiasDTO construirPrevisao(PrevisaoRiscoResponse risco) {
        return montadorPrevisaoOperacional.montar(risco);
    }

    private String montarContextoAtual(int suspeitasDia, String nomeMunicipio, String classificacao, String tendencia) {
        return montadorContextoOperacional.montarContextoAtual(suspeitasDia, nomeMunicipio, classificacao, tendencia);
    }

    private String montarPadraoHistorico(String comparativoHistorico) {
        return montadorContextoOperacional.montarPadraoHistorico(comparativoHistorico);
    }

    private List<String> montarChecklistInformativo(String nivelAtencao,
                                                    ContextoEpidemiologicoDTO contexto,
                                                    PrevisaoProximosDiasDTO previsao) {
        return checklistOperacionalPolicy.montarChecklist(nivelAtencao, contexto, previsao);
    }
}
