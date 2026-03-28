package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.ClimaPort;
import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.application.port.RedeAssistencialPort;
import br.com.fiap.vigisus.domain.geografia.CatalogoGeograficoBrasil;
import br.com.fiap.vigisus.domain.risco.CalculadoraRiscoClimatico;
import br.com.fiap.vigisus.domain.risco.ClassificacaoRiscoAgregadoPolicy;
import br.com.fiap.vigisus.domain.risco.MetricasRiscoClimatico;
import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.RiscoDiarioDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class RiscoAgregadoService {

    private static final List<String> UFS_BRASIL = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final MunicipioPort municipioPort;
    private final CasoDenguePort casoDenguePort;
    private final RedeAssistencialPort redeAssistencialPort;
    private final ClimaPort climaService;
    private final CatalogoGeograficoBrasil catalogoGeograficoBrasil;
    private final CalculadoraRiscoClimatico calculadoraRiscoClimatico;
    private final ClassificacaoRiscoAgregadoPolicy classificacaoRiscoPolicy;

    @Cacheable(value = "risco-brasil", key = "'todos'")
    public PrevisaoRiscoResponse calcularRiscoBrasil() {
        log.info("[RiscoBrasil] Calculando risco agregado do Brasil");

        List<Municipio> todosMunicipios = municipioPort.findAll();
        double[] coordMedia = calcularCoordenadaMedia(todosMunicipios);
        log.info("[RiscoBrasil] Coordenada media: lat={}, lon={}", coordMedia[0], coordMedia[1]);

        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(coordMedia[0], coordMedia[1]);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(coordMedia[0], coordMedia[1]);
        MetricasRiscoClimatico metricas = calculadoraRiscoClimatico.extrairMetricas(climaAtual, previsao16Dias);

        int score = calculadoraRiscoClimatico.calcularScore(metricas);
        List<String> fatores = montarFatores(metricas);
        String classificacao = classificacaoRiscoPolicy.classificar(score);
        List<RiscoDiarioDTO> risco14Dias = calcularRisco14Dias(previsao16Dias);
        double incidenciaHistorica = calcularIncidenciaMediaBrasil(2024);

        log.info("[RiscoBrasil] Score={}, Classificacao={}, Incidencia={}",
                score, classificacao, incidenciaHistorica);

        return PrevisaoRiscoResponse.builder()
                .coIbge("00")
                .municipio("Brasil")
                .score(score)
                .classificacao(classificacao)
                .incidencia(incidenciaHistorica)
                .fatores(fatores)
                .risco14Dias(risco14Dias)
                .build();
    }

    @Cacheable(value = "risco-estado", key = "#uf")
    public PrevisaoRiscoResponse calcularRiscoEstado(String uf) {
        log.info("[RiscoEstado] Calculando risco agregado para {}", uf);
        String ufUpper = uf.toUpperCase();

        List<Municipio> municipiosEstado = municipioPort.findBySgUf(ufUpper);
        if (municipiosEstado.isEmpty()) {
            log.warn("[RiscoEstado] Nenhum municipio encontrado para {}", uf);
            return null;
        }

        double[] coordMedia = calcularCoordenadaMedia(municipiosEstado);
        log.info("[RiscoEstado] {} - Coordenada media: lat={}, lon={}", uf, coordMedia[0], coordMedia[1]);

        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(coordMedia[0], coordMedia[1]);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(coordMedia[0], coordMedia[1]);
        MetricasRiscoClimatico metricas = calculadoraRiscoClimatico.extrairMetricas(climaAtual, previsao16Dias);

        int score = calculadoraRiscoClimatico.calcularScore(metricas);
        List<String> fatores = montarFatores(metricas);
        String classificacao = classificacaoRiscoPolicy.classificar(score);
        List<RiscoDiarioDTO> risco14Dias = calcularRisco14Dias(previsao16Dias);
        double incidenciaHistorica = calcularIncidenciaMediaEstado(ufUpper, 2024);

        log.info("[RiscoEstado] {} - Score={}, Classificacao={}, Incidencia={}",
                uf, score, classificacao, incidenciaHistorica);

        return PrevisaoRiscoResponse.builder()
                .coIbge(ufUpper)
                .municipio(catalogoGeograficoBrasil.nomeEstado(ufUpper))
                .score(score)
                .classificacao(classificacao)
                .incidencia(incidenciaHistorica)
                .fatores(fatores)
                .risco14Dias(risco14Dias)
                .build();
    }

    @Cacheable(value = "hospitais-brasil", key = "'todos'")
    public List<Estabelecimento> buscarHospitaisBrasil() {
        log.info("[HospitaisBrasil] Buscando hospitais das capitais");

        List<Estabelecimento> hospitaisCapitais = new ArrayList<>();
        for (String uf : UFS_BRASIL) {
            String coMunicipio = catalogoGeograficoBrasil.codigoCapitalRiscoAgregado(uf);
            if (coMunicipio != null) {
                hospitaisCapitais.addAll(redeAssistencialPort.buscarEstabelecimentosPorMunicipio(coMunicipio));
            }
        }

        log.info("[HospitaisBrasil] Total de hospitais das capitais: {}", hospitaisCapitais.size());
        return hospitaisCapitais;
    }

    @Cacheable(value = "hospitais-estado", key = "#uf")
    public List<Estabelecimento> buscarHospitaisEstado(String uf) {
        log.info("[HospitaisEstado] Buscando hospitais da capital + regiao ({})", uf);

        String coCapital = catalogoGeograficoBrasil.codigoCapitalRiscoAgregado(uf);
        if (coCapital == null) {
            log.warn("[HospitaisEstado] Capital nao encontrada para {}", uf);
            return Collections.emptyList();
        }

        Municipio capital = municipioPort.findByCoIbge(coCapital).orElse(null);
        if (capital == null || capital.getNuLatitude() == null || capital.getNuLongitude() == null) {
            log.warn("[HospitaisEstado] Coordenadas da capital nao encontradas para {}", uf);
            return Collections.emptyList();
        }

        double latCapital = capital.getNuLatitude();
        double lonCapital = capital.getNuLongitude();

        List<Estabelecimento> hospitaisOrdenados = redeAssistencialPort.buscarEstabelecimentosPorEstado(uf.toUpperCase()).stream()
                .filter(hospital -> hospital.getNuLatitude() != null && hospital.getNuLongitude() != null)
                .sorted(Comparator.comparing(hospital ->
                        calcularDistanciaKm(latCapital, lonCapital, hospital.getNuLatitude(), hospital.getNuLongitude())))
                .collect(Collectors.toList());

        List<Estabelecimento> dentroRaio = hospitaisOrdenados.stream()
                .filter(hospital ->
                        calcularDistanciaKm(latCapital, lonCapital, hospital.getNuLatitude(), hospital.getNuLongitude())
                                <= 100.0)
                .collect(Collectors.toList());

        if (!dentroRaio.isEmpty()) {
            log.info("[HospitaisEstado] {} - Total dentro de 100km: {}", uf, dentroRaio.size());
            return dentroRaio;
        }

        List<Estabelecimento> fallbackMaisProximos = hospitaisOrdenados.stream()
                .limit(20)
                .collect(Collectors.toList());

        log.info("[HospitaisEstado] {} - Nenhum hospital em 100km. Retornando {} mais proximos.",
                uf, fallbackMaisProximos.size());
        return fallbackMaisProximos;
    }

    @Cacheable(value = "incidencia-brasil", key = "#ano")
    public double calcularIncidenciaMediaBrasil(int ano) {
        log.debug("[IncidenciaBrasil] Calculando incidencia historica media do Brasil (ano={})", ano);

        List<Object[]> todosOsDados = casoDenguePort.agregaCasosPorMunicipioNoAno(ano);
        if (todosOsDados == null || todosOsDados.isEmpty()) {
            log.warn("[IncidenciaBrasil] Nenhum dado encontrado para ano {}", ano);
            return 0.0;
        }

        Map<String, Long> populacaoPorMunicipio = municipioPort.findAll().stream()
                .collect(Collectors.toMap(
                        Municipio::getCoIbge,
                        municipio -> municipio.getPopulacao() != null ? municipio.getPopulacao() : 0L,
                        (atual, ignorado) -> atual
                ));

        double somaIncidencia = 0.0;
        int quantidadeMunicipiosValidos = 0;

        for (Object[] row : todosOsDados) {
            if (row == null || row.length < 2 || row[0] == null || !(row[1] instanceof Number totalCasosNumber)) {
                continue;
            }

            String coMunicipio = String.valueOf(row[0]);
            long populacao = populacaoPorMunicipio.getOrDefault(coMunicipio, 0L);
            if (populacao <= 0) {
                continue;
            }

            double incidencia = (double) totalCasosNumber.longValue() / populacao * 100_000;
            somaIncidencia += incidencia;
            quantidadeMunicipiosValidos++;
        }

        double media = quantidadeMunicipiosValidos > 0 ? somaIncidencia / quantidadeMunicipiosValidos : 0.0;
        log.debug("[IncidenciaBrasil] Incidencia media = {}", media);
        return media;
    }

    @Cacheable(value = "incidencia-estado", key = "#uf + '-' + #ano")
    public double calcularIncidenciaMediaEstado(String uf, int ano) {
        log.debug("[IncidenciaEstado] Calculando incidencia historica media de {} (ano={})", uf, ano);

        List<Object[]> dadosEstado = casoDenguePort.agregaCasosPorEstadoNoAno(ano);
        if (dadosEstado == null || dadosEstado.isEmpty()) {
            log.warn("[IncidenciaEstado] Nenhum dado encontrado para {}, ano {}", uf, ano);
            return 0.0;
        }

        double somaIncidencia = 0.0;
        int quantidadeRegistrosValidos = 0;
        String ufUpper = uf.toUpperCase();

        for (Object[] row : dadosEstado) {
            String sgUf = (String) row[0];
            if (!ufUpper.equals(sgUf)) {
                continue;
            }

            long totalCasos = ((Number) row[1]).longValue();
            long populacao = ((Number) row[2]).longValue();
            if (populacao <= 0) {
                continue;
            }

            double incidencia = (double) totalCasos / populacao * 100_000;
            somaIncidencia += incidencia;
            quantidadeRegistrosValidos++;
        }

        double media = quantidadeRegistrosValidos > 0 ? somaIncidencia / quantidadeRegistrosValidos : 0.0;
        log.debug("[IncidenciaEstado] Incidencia media = {}", media);
        return media;
    }

    private double[] calcularCoordenadaMedia(List<Municipio> municipios) {
        double somaLatitudes = 0;
        double somaLongitudes = 0;
        int quantidadeMunicipiosComCoordenada = 0;

        for (Municipio municipio : municipios) {
            Double lat = municipio.getNuLatitude();
            Double lon = municipio.getNuLongitude();
            if (lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) {
                somaLatitudes += lat;
                somaLongitudes += lon;
                quantidadeMunicipiosComCoordenada++;
            }
        }

        if (quantidadeMunicipiosComCoordenada == 0) {
            return new double[]{-14.235, -51.925};
        }

        return new double[]{
                somaLatitudes / quantidadeMunicipiosComCoordenada,
                somaLongitudes / quantidadeMunicipiosComCoordenada
        };
    }

    private List<String> montarFatores(MetricasRiscoClimatico metricas) {
        List<String> fatores = new ArrayList<>();

        if (metricas.temperaturaAtual() >= 28) {
            fatores.add("Temperatura atual \u2265 28\u00B0C (" + String.format("%.1f", metricas.temperaturaAtual()) + "\u00B0C)");
        } else if (metricas.temperaturaAtual() >= 25) {
            fatores.add("Temperatura atual \u2265 25\u00B0C (" + String.format("%.1f", metricas.temperaturaAtual()) + "\u00B0C)");
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

    private List<RiscoDiarioDTO> calcularRisco14Dias(List<PrevisaoDiariaDTO> previsao) {
        LocalDate hoje = LocalDate.now();
        return calculadoraRiscoClimatico.calcularRiscoDiario(
                previsao,
                classificacaoRiscoPolicy,
                (indice, dia) -> hoje.plusDays(indice).toString()
        );
    }

    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final int raioTerraKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioTerraKm * c;
    }
}
