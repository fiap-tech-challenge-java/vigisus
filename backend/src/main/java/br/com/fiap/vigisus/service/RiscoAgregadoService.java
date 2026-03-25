package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;
import br.com.fiap.vigisus.dto.RiscoDiarioDTO;
import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para calcular RISCO AGREGADO em nível Brasil e Estado.
 *
 * O risco é calculado com base em dados climáticos (temperatura, chuva, umidade)
 * da região agregada, usando a coordenada média como ponto central.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiscoAgregadoService {

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ClimaService climaService;
    private final PrevisaoRiscoService previsaoRiscoService;

    // ─────────────────────────────────────────────────────────────────────
    // RISCO BRASIL
    // ─────────────────────────────────────────────────────────────────────

    @Cacheable(value = "risco-brasil", key = "'todos'")
    public PrevisaoRiscoResponse calcularRiscoBrasil() {
        log.info("[RiscoBrasil] Calculando risco agregado do Brasil");

        // 1. Buscar coordenada média do Brasil
        List<Municipio> todosMunicipios = municipioRepository.findAll();
        double[] coordMedia = calcularCoordenadaMedia(todosMunicipios);

        log.info("[RiscoBrasil] Coordenada média: lat={}, lon={}", coordMedia[0], coordMedia[1]);

        // 2. Buscar clima da coordenada média
        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(coordMedia[0], coordMedia[1]);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(coordMedia[0], coordMedia[1]);

        // 3. Calcular score (mesmo algoritmo que município)
        List<String> fatores = new ArrayList<>();
        int score = calcularScore(climaAtual, previsao16Dias, fatores);
        String classificacao = classificar(score);

        // 4. Calcular risco dos 14 dias
        List<RiscoDiarioDTO> risco14Dias = calcularRisco14Dias(previsao16Dias);

        // 5. Calcular incidência histórica agregada (CACHE)
        double incidenciaHistorica = calcularIncidenciaMediaBrasil(2024);

        log.info("[RiscoBrasil] Score={}, Classificação={}, Incidência={}", score, classificacao, incidenciaHistorica);

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

    // ─────────────────────────────────────────────────────────────────────
    // RISCO ESTADO
    // ─────────────────────────────────────────────────────────────────────

    @Cacheable(value = "risco-estado", key = "#uf")
    public PrevisaoRiscoResponse calcularRiscoEstado(String uf) {
        log.info("[RiscoEstado] Calculando risco agregado para {}", uf);

        // 1. Buscar municípios do estado
        List<Municipio> municipiosEstado = municipioRepository.findBySgUf(uf.toUpperCase());
        if (municipiosEstado.isEmpty()) {
            log.warn("[RiscoEstado] Nenhum município encontrado para {}", uf);
            return null;
        }

        // 2. Coordenada média do estado
        double[] coordMedia = calcularCoordenadaMedia(municipiosEstado);
        log.info("[RiscoEstado] {} - Coordenada média: lat={}, lon={}", uf, coordMedia[0], coordMedia[1]);

        // 3. Buscar clima
        ClimaAtualDTO climaAtual = climaService.buscarClimaAtual(coordMedia[0], coordMedia[1]);
        List<PrevisaoDiariaDTO> previsao16Dias = climaService.buscarPrevisao16Dias(coordMedia[0], coordMedia[1]);

        // 4. Calcular score
        List<String> fatores = new ArrayList<>();
        int score = calcularScore(climaAtual, previsao16Dias, fatores);
        String classificacao = classificar(score);

        // 5. Risco 14 dias
        List<RiscoDiarioDTO> risco14Dias = calcularRisco14Dias(previsao16Dias);

        // 6. Calcular incidência histórica agregada (CACHE)
        double incidenciaHistorica = calcularIncidenciaMediaEstado(uf, 2024);

        log.info("[RiscoEstado] {} - Score={}, Classificação={}, Incidência={}", uf, score, classificacao, incidenciaHistorica);

        return PrevisaoRiscoResponse.builder()
                .coIbge(uf)
                .municipio(obterNomeEstado(uf))
                .score(score)
                .classificacao(classificacao)
                .incidencia(incidenciaHistorica)
                .fatores(fatores)
                .risco14Dias(risco14Dias)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HOSPITAIS AGREGADOS (COM CACHE)
    // ─────────────────────────────────────────────────────────────────────

    @Cacheable(value = "hospitais-brasil", key = "'todos'")
    public List<Estabelecimento> buscarHospitaisBrasil() {
        log.info("[HospitaisBrasil] Buscando hospitais das capitais");

        // Mapa de código de município das capitais por UF
        Map<String, String> capitaisPorUF = obterCodigosCapitais();

        List<Estabelecimento> todasAsCapitais = new ArrayList<>();
        for (String coMunicipio : capitaisPorUF.values()) {
            List<Estabelecimento> hospitaisCapital = estabelecimentoRepository.findByMunicipio(coMunicipio);
            todasAsCapitais.addAll(hospitaisCapital);
        }

        log.info("[HospitaisBrasil] Total de hospitais das capitais: {}", todasAsCapitais.size());
        return todasAsCapitais;
    }

    @Cacheable(value = "hospitais-estado", key = "#uf")
    public List<Estabelecimento> buscarHospitaisEstado(String uf) {
        log.info("[HospitaisEstado] Buscando hospitais da capital + região ({})", uf);

        // 1. Achar a capital do estado
        String coCapital = obterCodigosCapitais().get(uf.toUpperCase());
        if (coCapital == null) {
            log.warn("[HospitaisEstado] Capital não encontrada para {}", uf);
            return Collections.emptyList();
        }

        // 2. Buscar coordenadas da capital
        Municipio capital = municipioRepository.findByCoIbge(coCapital).orElse(null);
        if (capital == null || capital.getNuLatitude() == null || capital.getNuLongitude() == null) {
            log.warn("[HospitaisEstado] Coordenadas da capital não encontradas para {}", uf);
            return Collections.emptyList();
        }

        double latCapital = capital.getNuLatitude();
        double lonCapital = capital.getNuLongitude();

        // 3. Buscar todos os hospitais do estado
        List<Estabelecimento> hospitaisEstado = estabelecimentoRepository.findByEstado(uf.toUpperCase());

        // 4. Ordenar por distância da capital
        List<Estabelecimento> hospitaisOrdenados = hospitaisEstado.stream()
            .filter(h -> h.getNuLatitude() != null && h.getNuLongitude() != null)
            .sorted(Comparator.comparing(h ->
                calcularDistanciaKm(latCapital, lonCapital, h.getNuLatitude(), h.getNuLongitude())))
            .collect(Collectors.toList());

        // 5. Preferir raio de 100 km; se vazio, retornar os mais próximos do estado
        List<Estabelecimento> dentroRaio = hospitaisOrdenados.stream()
            .filter(h -> calcularDistanciaKm(latCapital, lonCapital, h.getNuLatitude(), h.getNuLongitude()) <= 100.0)
            .collect(Collectors.toList());

        if (!dentroRaio.isEmpty()) {
            log.info("[HospitaisEstado] {} - Total dentro de 100km: {}", uf, dentroRaio.size());
            return dentroRaio;
        }

        List<Estabelecimento> fallbackMaisProximos = hospitaisOrdenados.stream()
            .limit(20)
            .collect(Collectors.toList());

        log.info("[HospitaisEstado] {} - Nenhum hospital em 100km. Retornando {} mais próximos.",
            uf, fallbackMaisProximos.size());
        return fallbackMaisProximos;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INCIDÊNCIA HISTÓRICA AGREGADA (COM CACHE)
    // ─────────────────────────────────────────────────────────────────────

    @Cacheable(value = "incidencia-brasil", key = "#ano")
    public double calcularIncidenciaMediaBrasil(int ano) {
        log.debug("[IncidenciaBrasil] Calculando incidência histórica média do Brasil (ano={})", ano);

        List<Object[]> todosOsDados = casoDengueRepository.agregaCasosPorMunicipioNoAno(ano);

        if (todosOsDados == null || todosOsDados.isEmpty()) {
            log.warn("[IncidenciaBrasil] Nenhum dado encontrado para ano {}", ano);
            return 0.0;
        }

        // Query otimizada retorna [coMunicipio, totalCasos].
        // População vem da base de municípios para evitar erro de índice e manter cálculo consistente.
        Map<String, Long> populacaoPorMunicipio = municipioRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Municipio::getCoIbge,
                        m -> m.getPopulacao() != null ? m.getPopulacao() : 0L,
                        (a, b) -> a
                ));

        double somaIncidencia = 0.0;
        int count = 0;

        for (Object[] row : todosOsDados) {
            if (row == null || row.length < 2 || row[0] == null || !(row[1] instanceof Number)) {
                continue;
            }

            String coMunicipio = String.valueOf(row[0]);
            long totalCasos = ((Number) row[1]).longValue();
            long populacao = populacaoPorMunicipio.getOrDefault(coMunicipio, 0L);

            if (populacao > 0) {
                double incidencia = (double) totalCasos / populacao * 100_000;
                somaIncidencia += incidencia;
                count++;
            }
        }

        double media = count > 0 ? somaIncidencia / count : 0.0;
        log.debug("[IncidenciaBrasil] Incidência média = {}", media);
        return media;
    }

    @Cacheable(value = "incidencia-estado", key = "#uf + '-' + #ano")
    public double calcularIncidenciaMediaEstado(String uf, int ano) {
        log.debug("[IncidenciaEstado] Calculando incidência histórica média de {} (ano={})", uf, ano);

        List<Object[]> dadosEstado = casoDengueRepository.agregaCasosPorEstadoNoAno(ano);

        if (dadosEstado == null || dadosEstado.isEmpty()) {
            log.warn("[IncidenciaEstado] Nenhum dado encontrado para {}, ano {}", uf, ano);
            return 0.0;
        }

        double somaIncidencia = 0.0;
        int count = 0;
        String ufUpper = uf.toUpperCase();

        for (Object[] row : dadosEstado) {
            // row: [sgUf, totalCasos, populacao, ...]
            String sgUf = (String) row[0];

            if (ufUpper.equals(sgUf)) {
                long totalCasos = ((Number) row[1]).longValue();
                long populacao = ((Number) row[2]).longValue();

                if (populacao > 0) {
                    double incidencia = (double) totalCasos / populacao * 100_000;
                    somaIncidencia += incidencia;
                    count++;
                }
            }
        }

        double media = count > 0 ? somaIncidencia / count : 0.0;
        log.debug("[IncidenciaEstado] Incidência média = {}", media);
        return media;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Calcula a coordenada (latitude, longitude) média de uma lista de municípios
     */
    private double[] calcularCoordenadaMedia(List<Municipio> municipios) {
        double sumLat = 0;
        double sumLon = 0;
        int count = 0;

        for (Municipio m : municipios) {
            Double lat = m.getNuLatitude();
            Double lon = m.getNuLongitude();

            // Validar coordenadas
            if (lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) {
                sumLat += lat;
                sumLon += lon;
                count++;
            }
        }

        if (count == 0) {
            // Fallback: coordenada do Brasil inteiro
            return new double[]{-14.235, -51.925};
        }

        return new double[]{sumLat / count, sumLon / count};
    }

    /**
     * Calcula score baseado no clima (mesmo algoritmo de PrevisaoRiscoService)
     */
    private int calcularScore(ClimaAtualDTO climaAtual, List<PrevisaoDiariaDTO> previsao16Dias,
                              List<String> fatores) {
        int score = 0;

        // Temperatura atual
        double tempAtual = climaAtual.getTemperatura() != null ? climaAtual.getTemperatura() : 0.0;
        if (tempAtual >= 28) {
            score += 2;
            fatores.add("Temperatura atual ≥ 28°C (" + String.format("%.1f", tempAtual) + "°C)");
        } else if (tempAtual >= 25) {
            score += 1;
            fatores.add("Temperatura atual ≥ 25°C (" + String.format("%.1f", tempAtual) + "°C)");
        }

        // Umidade atual
        int umidade = climaAtual.getUmidade() != null ? climaAtual.getUmidade() : 0;
        if (umidade >= 80) {
            score += 1;
            fatores.add("Umidade relativa ≥ 80% (" + umidade + "%)");
        }

        // Próximos 14 dias
        List<PrevisaoDiariaDTO> proximos14 = previsao16Dias.stream()
                .limit(14)
                .collect(Collectors.toList());

        // Temperatura média 14 dias
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

        // Chuva total 14 dias
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

        // Probabilidade média de chuva
        double probChuvaMedia = proximos14.stream()
                .mapToDouble(d -> d.getProbabilidadeChuva() != null ? d.getProbabilidadeChuva() : 0)
                .average()
                .orElse(0.0);

        if (probChuvaMedia >= 60) {
            score += 1;
            fatores.add(String.format("Probabilidade média de chuva ≥ 60%% (%.0f%%)", probChuvaMedia));
        }

        return score;
    }

    /**
     * Calcula risco diário para os 14 primeiros dias
     */
    private List<RiscoDiarioDTO> calcularRisco14Dias(List<PrevisaoDiariaDTO> previsao) {
        LocalDate hoje = LocalDate.now();
        List<RiscoDiarioDTO> resultado = new ArrayList<>();

        // Tomar apenas os primeiros 14 dias
        int limite = Math.min(14, previsao.size());
        for (int i = 0; i < limite; i++) {
            PrevisaoDiariaDTO dia = previsao.get(i);

            double tempMax = dia.getTemperaturaMaxima() != null ? dia.getTemperaturaMaxima() : 0.0;
            double chuvaMm = dia.getPrecipitacaoTotal() != null ? dia.getPrecipitacaoTotal() : 0.0;
            double probChuva = dia.getProbabilidadeChuva() != null ? dia.getProbabilidadeChuva() : 0.0;

            int scoreDia = 0;
            if (tempMax >= 28) scoreDia += 2;
            else if (tempMax >= 25) scoreDia += 1;
            if (chuvaMm >= 20) scoreDia += 2;
            else if (chuvaMm >= 10) scoreDia += 1;
            if (probChuva >= 60) scoreDia += 1;

            String classificacaoDia = classificar(scoreDia);
            LocalDate dataDia = hoje.plusDays(i);

            resultado.add(RiscoDiarioDTO.builder()
                    .data(dataDia.toString())
                    .scoreDia(scoreDia)
                    .classificacao(classificacaoDia)
                    .tempMax(tempMax)
                    .chuvaMm(chuvaMm)
                    .probChuva(probChuva)
                    .build());
        }

        return resultado;
    }

    private String classificar(int score) {
        if (score <= 1) {
            return "BAIXO";
        } else if (score <= 3) {
            return "MODERADO";
        } else if (score <= 5) {
            return "ALTO";
        } else {
            return "EPIDEMIA";
        }
    }

    private String obterNomeEstado(String sgUf) {
        Map<String, String> estados = new HashMap<>();
        estados.put("AC", "Acre");
        estados.put("AL", "Alagoas");
        estados.put("AP", "Amapá");
        estados.put("AM", "Amazonas");
        estados.put("BA", "Bahia");
        estados.put("CE", "Ceará");
        estados.put("DF", "Distrito Federal");
        estados.put("ES", "Espírito Santo");
        estados.put("GO", "Goiás");
        estados.put("MA", "Maranhão");
        estados.put("MT", "Mato Grosso");
        estados.put("MS", "Mato Grosso do Sul");
        estados.put("MG", "Minas Gerais");
        estados.put("PA", "Pará");
        estados.put("PB", "Paraíba");
        estados.put("PR", "Paraná");
        estados.put("PE", "Pernambuco");
        estados.put("PI", "Piauí");
        estados.put("RJ", "Rio de Janeiro");
        estados.put("RN", "Rio Grande do Norte");
        estados.put("RS", "Rio Grande do Sul");
        estados.put("RO", "Rondônia");
        estados.put("RR", "Roraima");
        estados.put("SC", "Santa Catarina");
        estados.put("SP", "São Paulo");
        estados.put("SE", "Sergipe");
        estados.put("TO", "Tocantins");
        return estados.getOrDefault(sgUf, sgUf);
    }

    /**
     * Retorna mapa de código de município das capitais por UF
     * Fonte: IBGE
     */
    private Map<String, String> obterCodigosCapitais() {
        Map<String, String> capitais = new HashMap<>();
        capitais.put("AC", "1100015");  // Rio Branco
        capitais.put("AL", "2700104");  // Maceió
        capitais.put("AP", "1600055");  // Macapá
        capitais.put("AM", "1302603");  // Manaus
        capitais.put("BA", "2902404");  // Salvador
        capitais.put("CE", "2304400");  // Fortaleza
        capitais.put("DF", "5300108");  // Brasília
        capitais.put("ES", "3200100");  // Vitória
        capitais.put("GO", "5208707");  // Goiânia
        capitais.put("MA", "2111300");  // São Luís
        capitais.put("MT", "5103403");  // Cuiabá
        capitais.put("MS", "5002704");  // Campo Grande
        capitais.put("MG", "3106200");  // Belo Horizonte
        capitais.put("PA", "1505714");  // Belém
        capitais.put("PB", "2507002");  // João Pessoa
        capitais.put("PR", "4106902");  // Curitiba
        capitais.put("PE", "2611606");  // Recife
        capitais.put("PI", "2211001");  // Teresina
        capitais.put("RJ", "3304557");  // Rio de Janeiro
        capitais.put("RN", "2408102");  // Natal
        capitais.put("RS", "4314902");  // Porto Alegre
        capitais.put("RO", "1100122");  // Porto Velho
        capitais.put("RR", "1400100");  // Boa Vista
        capitais.put("SC", "4205402");  // Florianópolis
        capitais.put("SP", "3550308");  // São Paulo
        capitais.put("SE", "2800308");  // Aracaju
        capitais.put("TO", "2710202");  // Palmas
        return capitais;
    }

    /**
     * Calcula distância em km entre duas coordenadas (fórmula de Haversine)
     */
    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;  // Raio da Terra em km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;  // Distância em km
    }
}
