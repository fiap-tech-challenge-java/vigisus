package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.RankingMunicipioDTO;
import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int MAX_RANKING_LIMIT = 100;
    private static final double THRESHOLD_MODERADO = 50.0;
    private static final double THRESHOLD_ALTO = 100.0;
    private static final double THRESHOLD_EPIDEMIA = 300.0;

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;

    public RankingResponse calcularRanking(String uf, String doenca, int ano, int top, String ordem) {
        int topLimitado = Math.min(top, MAX_RANKING_LIMIT);

        List<Municipio> municipios = municipioRepository.findBySgUf(uf.toUpperCase());

        List<String> cosMunicipios = municipios.stream()
                .filter(m -> m.getPopulacao() != null && m.getPopulacao() > 0)
                .map(Municipio::getCoIbge)
                .collect(Collectors.toList());

        Map<String, Long> totalPorMunicipio = buscarTotaisPorMunicipio(cosMunicipios, ano);

        List<RankingMunicipioDTO> rankingCompleto = new ArrayList<>();
        for (Municipio m : municipios) {
            if (m.getPopulacao() == null || m.getPopulacao() <= 0) {
                continue;
            }
            long totalCasos = totalPorMunicipio.getOrDefault(m.getCoIbge(), 0L);
            double incidencia = (double) totalCasos / m.getPopulacao() * 100_000;

            rankingCompleto.add(RankingMunicipioDTO.builder()
                    .coIbge(m.getCoIbge())
                    .municipio(m.getNoMunicipio())
                    .totalCasos(totalCasos)
                    .populacao(m.getPopulacao())
                    .incidencia100k(incidencia)
                    .classificacao(classificar(incidencia))
                    .build());
        }

        Comparator<RankingMunicipioDTO> comparator = Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k);
        if (!"melhores".equalsIgnoreCase(ordem)) {
            comparator = comparator.reversed();
        }
        rankingCompleto.sort(comparator);

        for (int i = 0; i < rankingCompleto.size(); i++) {
            rankingCompleto.get(i).setPosicao(i + 1);
        }

        int totalMunicipiosComDados = rankingCompleto.size();

        List<RankingMunicipioDTO> topRanking = rankingCompleto.stream()
                .limit(topLimitado)
                .collect(Collectors.toList());

        return RankingResponse.builder()
                .uf(uf.toUpperCase())
                .doenca(doenca)
                .ano(ano)
                .totalMunicipiosComDados(totalMunicipiosComDados)
                .ranking(topRanking)
                .build();
    }

    public String calcularPosicaoNoEstado(String coIbge, String uf, String doenca, int ano) {
        List<Municipio> municipios = municipioRepository.findBySgUf(uf.toUpperCase());

        List<String> cosMunicipios = municipios.stream()
                .filter(m -> m.getPopulacao() != null && m.getPopulacao() > 0)
                .map(Municipio::getCoIbge)
                .collect(Collectors.toList());

        if (cosMunicipios.isEmpty()) {
            return null;
        }

        Map<String, Long> totalPorMunicipio = buscarTotaisPorMunicipio(cosMunicipios, ano);

        List<RankingMunicipioDTO> rankingCompleto = new ArrayList<>();
        for (Municipio m : municipios) {
            if (m.getPopulacao() == null || m.getPopulacao() <= 0) {
                continue;
            }
            long totalCasos = totalPorMunicipio.getOrDefault(m.getCoIbge(), 0L);
            double incidencia = (double) totalCasos / m.getPopulacao() * 100_000;

            rankingCompleto.add(RankingMunicipioDTO.builder()
                    .coIbge(m.getCoIbge())
                    .incidencia100k(incidencia)
                    .build());
        }

        rankingCompleto.sort(Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k).reversed());

        int total = rankingCompleto.size();
        for (int i = 0; i < total; i++) {
            if (coIbge.equals(rankingCompleto.get(i).getCoIbge())) {
                return (i + 1) + " de " + total;
            }
        }

        return null;
    }

    private Map<String, Long> buscarTotaisPorMunicipio(List<String> cosMunicipios, int ano) {
        Map<String, Long> totalPorMunicipio = new HashMap<>();
        if (!cosMunicipios.isEmpty()) {
            List<Object[]> resultados = casoDengueRepository
                    .sumTotalCasosByCoMunicipioInAndAno(cosMunicipios, ano);
            for (Object[] row : resultados) {
                totalPorMunicipio.put((String) row[0], ((Number) row[1]).longValue());
            }
        }
        return totalPorMunicipio;
    }

    private String classificar(double incidencia) {
        if (incidencia < THRESHOLD_MODERADO) {
            return "BAIXO";
        } else if (incidencia < THRESHOLD_ALTO) {
            return "MODERADO";
        } else if (incidencia <= THRESHOLD_EPIDEMIA) {
            return "ALTO";
        } else {
            return "EPIDEMIA";
        }
    }
}
