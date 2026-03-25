package br.com.fiap.vigisus.domain.risco;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.RiscoDiarioDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class CalculadoraRiscoClimatico {

    public MetricasRiscoClimatico extrairMetricas(ClimaAtualDTO climaAtual, List<PrevisaoDiariaDTO> previsao16Dias) {
        List<PrevisaoDiariaDTO> proximos14Dias = limitarA14Dias(previsao16Dias);

        double temperaturaAtual = climaAtual != null && climaAtual.getTemperatura() != null
                ? climaAtual.getTemperatura()
                : 0.0;
        int umidadeAtual = climaAtual != null && climaAtual.getUmidade() != null
                ? climaAtual.getUmidade()
                : 0;
        double temperaturaMedia14Dias = proximos14Dias.stream()
                .mapToDouble(dia -> valorSeguro(dia.getTemperaturaMaxima()))
                .average()
                .orElse(0.0);
        double chuvaTotal14Dias = proximos14Dias.stream()
                .mapToDouble(dia -> valorSeguro(dia.getPrecipitacaoTotal()))
                .sum();
        double probabilidadeMediaChuva14Dias = proximos14Dias.stream()
                .mapToDouble(dia -> valorSeguro(dia.getProbabilidadeChuva()))
                .average()
                .orElse(0.0);

        return new MetricasRiscoClimatico(
                temperaturaAtual,
                umidadeAtual,
                temperaturaMedia14Dias,
                chuvaTotal14Dias,
                probabilidadeMediaChuva14Dias
        );
    }

    public int calcularScore(MetricasRiscoClimatico metricas) {
        int score = 0;

        if (metricas.temperaturaAtual() >= 28) {
            score += 2;
        } else if (metricas.temperaturaAtual() >= 25) {
            score += 1;
        }

        if (metricas.umidadeAtual() >= 80) {
            score += 1;
        }

        if (metricas.temperaturaMedia14Dias() >= 28) {
            score += 2;
        } else if (metricas.temperaturaMedia14Dias() >= 25) {
            score += 1;
        }

        if (metricas.chuvaTotal14Dias() >= 100) {
            score += 2;
        } else if (metricas.chuvaTotal14Dias() >= 50) {
            score += 1;
        }

        if (metricas.probabilidadeMediaChuva14Dias() >= 60) {
            score += 1;
        }

        return score;
    }

    public List<RiscoDiarioDTO> calcularRiscoDiario(
            List<PrevisaoDiariaDTO> previsao,
            ClassificacaoRiscoPolicy classificacaoPolicy,
            BiFunction<Integer, PrevisaoDiariaDTO, String> resolvedorData
    ) {
        List<PrevisaoDiariaDTO> proximos14Dias = limitarA14Dias(previsao);
        List<RiscoDiarioDTO> riscoDiario = new ArrayList<>(proximos14Dias.size());

        for (int indice = 0; indice < proximos14Dias.size(); indice++) {
            PrevisaoDiariaDTO dia = proximos14Dias.get(indice);
            double temperaturaMaxima = valorSeguro(dia.getTemperaturaMaxima());
            double chuvaMm = valorSeguro(dia.getPrecipitacaoTotal());
            double probabilidadeChuva = valorSeguro(dia.getProbabilidadeChuva());

            int scoreDia = calcularScoreDiario(temperaturaMaxima, chuvaMm, probabilidadeChuva);
            riscoDiario.add(RiscoDiarioDTO.builder()
                    .data(resolvedorData.apply(indice, dia))
                    .scoreDia(scoreDia)
                    .classificacao(classificacaoPolicy.classificar(scoreDia))
                    .tempMax(temperaturaMaxima)
                    .chuvaMm(chuvaMm)
                    .probChuva(probabilidadeChuva)
                    .build());
        }

        return riscoDiario;
    }

    private List<PrevisaoDiariaDTO> limitarA14Dias(List<PrevisaoDiariaDTO> previsao) {
        if (previsao == null || previsao.isEmpty()) {
            return Collections.emptyList();
        }
        return previsao.stream().limit(14).toList();
    }

    private int calcularScoreDiario(double temperaturaMaxima, double chuvaMm, double probabilidadeChuva) {
        int score = 0;
        if (temperaturaMaxima >= 28) {
            score += 2;
        } else if (temperaturaMaxima >= 25) {
            score += 1;
        }
        if (chuvaMm >= 20) {
            score += 2;
        } else if (chuvaMm >= 10) {
            score += 1;
        }
        if (probabilidadeChuva >= 60) {
            score += 1;
        }
        return score;
    }

    private double valorSeguro(Number valor) {
        return valor != null ? valor.doubleValue() : 0.0;
    }
}
