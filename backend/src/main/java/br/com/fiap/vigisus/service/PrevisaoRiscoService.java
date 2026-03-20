package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.RiscoDiarioDTO;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrevisaoRiscoService {

    private final MunicipioService municipioService;
    private final ClimaService climaService;

    @Cacheable(value = "previsao-risco", key = "#coIbge")
    public PrevisaoRiscoResponse calcularRisco(String coIbge) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        Double lat = municipio.getNuLatitude();
        Double lon = municipio.getNuLongitude();

        log.info("[PrevisaoRisco] {} | lat={} lon={}", municipio.getNoMunicipio(), lat, lon);

        // (0.0, 0.0) is used as a sentinel value in the DB when coordinates are missing
        if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
            log.warn("[PrevisaoRisco] Coordenadas inválidas para {} — usando centroide do estado {}",
                    coIbge, municipio.getSgUf());
            double[] coord = getCentroideEstado(municipio.getSgUf());
            lat = coord[0];
            lon = coord[1];
        }

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
        List<RiscoDiarioDTO> risco14Dias = calcularRisco14Dias(previsao16Dias);

        return PrevisaoRiscoResponse.builder()
                .coIbge(coIbge)
                .municipio(municipio.getNoMunicipio())
                .score(score)
                .classificacao(classificacao)
                .fatores(fatores)
                .risco14Dias(risco14Dias)
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

    private List<RiscoDiarioDTO> calcularRisco14Dias(List<PrevisaoDiariaDTO> previsao) {
        return previsao.stream().limit(14).map(dia -> {
            double tempMax = dia.getTemperaturaMaxima() != null ? dia.getTemperaturaMaxima() : 0.0;
            double chuvaMm = dia.getPrecipitacaoTotal() != null ? dia.getPrecipitacaoTotal() : 0.0;
            double probChuva = dia.getProbabilidadeChuva() != null ? dia.getProbabilidadeChuva() : 0.0;

            int scoreDia = 0;
            if (tempMax >= 28) scoreDia += 2;
            else if (tempMax >= 25) scoreDia += 1;
            if (chuvaMm >= 20) scoreDia += 2;
            else if (chuvaMm >= 10) scoreDia += 1;
            if (probChuva >= 60) scoreDia += 1;

            String classificacaoDia = scoreDia <= 1 ? "BAIXO"
                    : scoreDia <= 3 ? "MODERADO"
                    : scoreDia <= 5 ? "ALTO"
                    : "MUITO_ALTO";

            return RiscoDiarioDTO.builder()
                    .data(dia.getData())
                    .scoreDia(scoreDia)
                    .classificacao(classificacaoDia)
                    .tempMax(tempMax)
                    .chuvaMm(chuvaMm)
                    .probChuva(probChuva)
                    .build();
        }).collect(Collectors.toList());
    }

    private double[] getCentroideEstado(String sgUf) {
        if (sgUf == null) {
            return new double[]{-15.7801, -47.9292};
        }
        return switch (sgUf) {
            case "MG" -> new double[]{-18.5122, -44.5550};
            case "SP" -> new double[]{-22.1875, -48.7966};
            case "RJ" -> new double[]{-22.2500, -42.6667};
            case "ES" -> new double[]{-19.1834, -40.3089};
            case "BA" -> new double[]{-12.5797, -41.7007};
            case "GO" -> new double[]{-15.9670, -49.8319};
            case "DF" -> new double[]{-15.7217, -47.9292};
            case "PR" -> new double[]{-24.8900, -51.5550};
            case "SC" -> new double[]{-27.2423, -50.2189};
            case "RS" -> new double[]{-30.0346, -51.2177};
            case "MT" -> new double[]{-12.6819, -56.9211};
            case "MS" -> new double[]{-20.7722, -54.7852};
            case "PA" -> new double[]{-3.4168,  -52.0000};
            case "AM" -> new double[]{-3.4168,  -65.0000};
            case "CE" -> new double[]{-5.4984,  -39.3206};
            case "PE" -> new double[]{-8.8137,  -36.9541};
            default   -> new double[]{-15.7801, -47.9292};
        };
    }
}
