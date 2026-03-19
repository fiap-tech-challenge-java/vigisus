package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrevisaoRiscoService {

    private final MunicipioService municipioService;
    private final ClimaService climaService;

    public PrevisaoRiscoResponse calcularRisco(String coIbge) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        double lat = municipio.getNuLatitude();
        double lon = municipio.getNuLongitude();

        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(lat, lon);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(lat, lon);

        List<String> fatores = new ArrayList<>();
        int score = 0;

        // Temperatura atual
        double tempAtual = climaAtual.getTemperatura() != null ? climaAtual.getTemperatura() : 0.0;
        if (tempAtual >= 28) {
            score += 2;
            fatores.add("Temperatura atual ≥ 28°C (" + tempAtual + "°C)");
        } else if (tempAtual >= 25) {
            score += 1;
            fatores.add("Temperatura atual ≥ 25°C (" + tempAtual + "°C)");
        }

        // Umidade atual
        int umidade = climaAtual.getUmidade() != null ? climaAtual.getUmidade() : 0;
        if (umidade >= 80) {
            score += 1;
            fatores.add("Umidade relativa ≥ 80% (" + umidade + "%)");
        }

        // Estatísticas dos 14 primeiros dias da previsão
        List<PrevisaoDiariaDTO> proximos14 = previsao16Dias.stream()
                .limit(14)
                .toList();

        double tempMedia14 = proximos14.stream()
                .mapToDouble(d -> d.getTemperaturaMaxima() != null ? d.getTemperaturaMaxima() : 0.0)
                .average()
                .orElse(0.0);

        if (tempMedia14 >= 28) {
            score += 2;
            fatores.add(String.format("Temp. média 14 dias ≥ 28°C (%.1f°C)", tempMedia14));
        } else if (tempMedia14 >= 25) {
            score += 1;
            fatores.add(String.format("Temp. média 14 dias ≥ 25°C (%.1f°C)", tempMedia14));
        }

        double chuvaTotal14 = proximos14.stream()
                .mapToDouble(d -> d.getPrecipitacaoTotal() != null ? d.getPrecipitacaoTotal() : 0.0)
                .sum();

        if (chuvaTotal14 >= 100) {
            score += 2;
            fatores.add(String.format("Chuva total 14 dias ≥ 100mm (%.1fmm)", chuvaTotal14));
        } else if (chuvaTotal14 >= 50) {
            score += 1;
            fatores.add(String.format("Chuva total 14 dias ≥ 50mm (%.1fmm)", chuvaTotal14));
        }

        double probChuvaMedia = proximos14.stream()
                .mapToDouble(d -> d.getProbabilidadeChuva() != null ? d.getProbabilidadeChuva() : 0)
                .average()
                .orElse(0.0);

        if (probChuvaMedia >= 60) {
            score += 1;
            fatores.add(String.format("Probabilidade média de chuva ≥ 60%% (%.0f%%)", probChuvaMedia));
        }

        String classificacao = classificar(score);

        return PrevisaoRiscoResponse.builder()
                .coIbge(coIbge)
                .municipio(municipio.getNoMunicipio())
                .score(score)
                .classificacao(classificacao)
                .fatores(fatores)
                .build();
    }

    private String classificar(int score) {
        if (score <= 1) {
            return "BAIXO";
        } else if (score <= 3) {
            return "MODERADO";
        } else if (score <= 5) {
            return "ALTO";
        } else {
            return "MUITO_ALTO";
        }
    }
}
