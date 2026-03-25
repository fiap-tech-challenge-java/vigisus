package br.com.fiap.vigisus.application.operacional;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.domain.epidemiologia.ComparativoHistoricoEpidemiologicoPolicy;
import br.com.fiap.vigisus.domain.epidemiologia.JanelaEpidemiologicaQuatroSemanas;
import br.com.fiap.vigisus.domain.epidemiologia.SemanaEpidemiologica;
import br.com.fiap.vigisus.domain.geografia.CoIbge;
import br.com.fiap.vigisus.domain.operacional.CalculadoraTendenciaOperacional;
import br.com.fiap.vigisus.dto.PressaoOperacionalResponse.ContextoEpidemiologicoDTO;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConstruirContextoEpidemiologicoOperacional {

    private final CasoDenguePort casoDenguePort;
    private final MunicipioPort municipioPort;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;
    private final ComparativoHistoricoEpidemiologicoPolicy comparativoHistoricoEpidemiologicoPolicy;
    private final CalculadoraTendenciaOperacional calculadoraTendenciaOperacional;

    public ContextoEpidemiologicoDTO executar(String coIbge) {
        return executar(CoIbge.of(coIbge));
    }

    public ContextoEpidemiologicoDTO executar(CoIbge coIbge) {
        String codigoMunicipio = coIbge.value();
        JanelaEpidemiologicaQuatroSemanas janela = JanelaEpidemiologicaQuatroSemanas.daData(LocalDate.now());
        int anoAtual = janela.anoAtual();

        Map<Integer, Long> casosSemanaAnoAtual = buscarCasosPorSemanas(
                codigoMunicipio,
                anoAtual,
                janela.semanasAnoAtual()
        );
        Map<Integer, Long> casosSemanaAnoAnterior = buscarCasosPorSemanas(
                codigoMunicipio,
                janela.anoAnterior(),
                janela.semanasAnoAnterior()
        );

        List<Long> casosSemanasOrdenadas = janela.semanasOrdenadas()
                .stream()
                .map(semana -> buscarTotalDaSemana(
                        semana,
                        anoAtual,
                        casosSemanaAnoAtual,
                        casosSemanaAnoAnterior
                ))
                .toList();

        long casosSemanasAtual = casosSemanasOrdenadas.get(casosSemanasOrdenadas.size() - 1);
        long casosSemana3Atras = casosSemanasOrdenadas.get(0);
        int casosUltimasSemanas = (int) casosSemanasOrdenadas.stream().mapToLong(Long::longValue).sum();

        String tendencia = calculadoraTendenciaOperacional.calcular(casosSemanasAtual, casosSemana3Atras);
        String classificacaoAtual = classificarPorTotal(codigoMunicipio, anoAtual);

        Map<Integer, Long> casosMesmoPeriodoAnoAnterior = buscarCasosPorSemanas(
                codigoMunicipio,
                janela.anoAnterior(),
                janela.semanasMesmoPeriodoAnoAnterior()
        );
        long totalAnoAnteriorMesmoPeriodo = casosMesmoPeriodoAnoAnterior.values().stream().mapToLong(Long::longValue).sum();

        String comparativoHistorico = comparativoHistoricoEpidemiologicoPolicy.gerarComparativo(
                casosUltimasSemanas,
                totalAnoAnteriorMesmoPeriodo,
                anoAtual
        );

        return ContextoEpidemiologicoDTO.builder()
                .classificacaoAtual(classificacaoAtual)
                .casosUltimasSemanas(casosUltimasSemanas)
                .tendencia(tendencia)
                .comparativoHistorico(comparativoHistorico)
                .build();
    }

    private Map<Integer, Long> buscarCasosPorSemanas(String coIbge, int ano, List<Integer> semanas) {
        Map<Integer, Long> casosPorSemana = new LinkedHashMap<>();
        if (semanas.isEmpty()) {
            return casosPorSemana;
        }

        List<Object[]> rows = casoDenguePort.findCasosPorSemanas(coIbge, ano, semanas);
        for (Object[] row : rows) {
            casosPorSemana.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return casosPorSemana;
    }

    private long buscarTotalDaSemana(SemanaEpidemiologica semana,
                                     int anoAtual,
                                     Map<Integer, Long> casosSemanaAnoAtual,
                                     Map<Integer, Long> casosSemanaAnoAnterior) {
        if (semana.ano() < anoAtual) {
            return casosSemanaAnoAnterior.getOrDefault(semana.numero(), 0L);
        }
        return casosSemanaAnoAtual.getOrDefault(semana.numero(), 0L);
    }

    private String classificarPorTotal(String coIbge, int ano) {
        long total = casoDenguePort.sumTotalCasosByCoMunicipioAndAno(coIbge, ano);
        if (total == 0) {
            return "BAIXO";
        }

        try {
            Municipio municipio = municipioPort.findByCoIbge(coIbge).orElseThrow();
            long populacao = municipio.getPopulacao() != null && municipio.getPopulacao() > 0
                    ? municipio.getPopulacao()
                    : 1L;
            double incidencia = (double) total / populacao * 100_000;
            return classificacaoEpidemiologicaPolicy.classificar(incidencia);
        } catch (Exception e) {
            return "BAIXO";
        }
    }
}
