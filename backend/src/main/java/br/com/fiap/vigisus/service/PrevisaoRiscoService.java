package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.ClimaPort;
import br.com.fiap.vigisus.domain.geografia.CatalogoGeograficoBrasil;
import br.com.fiap.vigisus.domain.risco.CalculadoraRiscoClimatico;
import br.com.fiap.vigisus.domain.risco.ClassificacaoRiscoMunicipioPolicy;
import br.com.fiap.vigisus.domain.risco.MetricasRiscoClimatico;
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
public class PrevisaoRiscoService {

    private final MunicipioService municipioService;
    private final ClimaPort climaService;
    private final CatalogoGeograficoBrasil catalogoGeograficoBrasil;
    private final CalculadoraRiscoClimatico calculadoraRiscoClimatico;
    private final ClassificacaoRiscoMunicipioPolicy classificacaoRiscoPolicy;

    @Cacheable(value = "previsao-risco", key = "#coIbge")
    public PrevisaoRiscoResponse calcularRisco(String coIbge) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        Double lat = municipio.getNuLatitude();
        Double lon = municipio.getNuLongitude();

        log.info("[PrevisaoRisco] {} | lat={} lon={}", municipio.getNoMunicipio(), lat, lon);

        if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
            log.warn("[PrevisaoRisco] Coordenadas inv\u00E1lidas para {} - usando centroide do estado {}",
                    coIbge, municipio.getSgUf());
            double[] coord = catalogoGeograficoBrasil.centroideEstado(municipio.getSgUf());
            lat = coord[0];
            lon = coord[1];
        }

        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(lat, lon);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(lat, lon);
        MetricasRiscoClimatico metricas = calculadoraRiscoClimatico.extrairMetricas(climaAtual, previsao16Dias);

        int score = calculadoraRiscoClimatico.calcularScore(metricas);
        List<String> fatores = montarFatores(metricas);
        String classificacao = classificacaoRiscoPolicy.classificar(score);
        List<RiscoDiarioDTO> risco14Dias = calculadoraRiscoClimatico.calcularRiscoDiario(
                previsao16Dias,
                classificacaoRiscoPolicy,
                (indice, dia) -> dia.getData()
        );

        return PrevisaoRiscoResponse.builder()
                .coIbge(coIbge)
                .municipio(municipio.getNoMunicipio())
                .uf(municipio.getSgUf())
                .temperaturaMedia(climaAtual.getTemperatura())
                .chuvaAcumulada(climaAtual.getPrecipitacao())
                .score(score)
                .classificacao(classificacao)
                .fatores(fatores)
                .risco14Dias(risco14Dias)
                .build();
    }

    private List<String> montarFatores(MetricasRiscoClimatico metricas) {
        List<String> fatores = new ArrayList<>();

        if (metricas.temperaturaAtual() >= 28) {
            fatores.add("Temperatura atual \u2265 28\u00B0C (" + metricas.temperaturaAtual() + "\u00B0C)");
        } else if (metricas.temperaturaAtual() >= 25) {
            fatores.add("Temperatura atual \u2265 25\u00B0C (" + metricas.temperaturaAtual() + "\u00B0C)");
        }

        if (metricas.umidadeAtual() >= 80) {
            fatores.add("Umidade relativa \u2265 80% (" + metricas.umidadeAtual() + "%)");
        }

        if (metricas.temperaturaMedia14Dias() >= 28) {
            fatores.add(String.format("Temp. m\u00E9dia 14 dias \u2265 28\u00B0C (%.1f\u00B0C)",
                    metricas.temperaturaMedia14Dias()));
        } else if (metricas.temperaturaMedia14Dias() >= 25) {
            fatores.add(String.format("Temp. m\u00E9dia 14 dias \u2265 25\u00B0C (%.1f\u00B0C)",
                    metricas.temperaturaMedia14Dias()));
        }

        if (metricas.chuvaTotal14Dias() >= 100) {
            fatores.add(String.format("Chuva total 14 dias \u2265 100mm (%.1fmm)", metricas.chuvaTotal14Dias()));
        } else if (metricas.chuvaTotal14Dias() >= 50) {
            fatores.add(String.format("Chuva total 14 dias \u2265 50mm (%.1fmm)", metricas.chuvaTotal14Dias()));
        }

        if (metricas.probabilidadeMediaChuva14Dias() >= 60) {
            fatores.add(String.format("Probabilidade m\u00E9dia de chuva \u2265 60%% (%.0f%%)",
                    metricas.probabilidadeMediaChuva14Dias()));
        }

        return fatores;
    }

}
