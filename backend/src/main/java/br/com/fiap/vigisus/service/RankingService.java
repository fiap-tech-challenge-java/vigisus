package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.RankingMunicipioDTO;
import br.com.fiap.vigisus.dto.RankingResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

    private static final int MAX_RANKING_LIMIT = 1000;
    private static final double THRESHOLD_MODERADO = 50.0;
    private static final double THRESHOLD_ALTO = 100.0;
    private static final double THRESHOLD_EPIDEMIA = 300.0;

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;

    @Cacheable(value = "ranking-municipal", key = "#uf + '-' + #doenca + '-' + #ano")
    public RankingResponse calcularRanking(String uf, String doenca, int ano, int top, String ordem) {
        int topLimitado = Math.min(top, MAX_RANKING_LIMIT);

        // ─────────────────────────────────────────────────────────────
        // OTIMIZAÇÃO: Uma ÚNICA query com JOIN em vez de N+1 queries
        // ─────────────────────────────────────────────────────────────
        List<Object[]> rankingData = casoDengueRepository.rankingOtimizadoPorEstado(uf.toUpperCase(), ano);

        List<RankingMunicipioDTO> rankingCompleto = rankingData.stream()
                .map(row -> {
                    String coIbge = (String) row[0];
                    String municipio = (String) row[1];
                    long totalCasos = ((Number) row[3]).longValue();
                    long populacao = ((Number) row[4]).longValue();
                    
                    double incidencia = populacao > 0 
                            ? (double) totalCasos / populacao * 100_000 
                            : 0;

                    return RankingMunicipioDTO.builder()
                            .coIbge(coIbge)
                            .municipio(municipio)
                            .totalCasos(totalCasos)
                            .populacao(populacao)
                            .incidencia100k(incidencia)
                            .classificacao(classificar(incidencia))
                            .build();
                })
                .collect(Collectors.toList());

        // Ordenar conforme solicitado
        Comparator<RankingMunicipioDTO> comparator = Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k);
        if ("melhores".equalsIgnoreCase(ordem)) {
            rankingCompleto.sort(comparator);
        } else {
            rankingCompleto.sort(comparator.reversed());
        }

        // Adicionar posição
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
        // ─────────────────────────────────────────────────────────────
        // OTIMIZAÇÃO: Uma query otimizada em vez de carregar municipios
        // ─────────────────────────────────────────────────────────────
        List<Object[]> rankingData = casoDengueRepository.rankingOtimizadoPorEstado(uf.toUpperCase(), ano);

        List<RankingMunicipioDTO> rankingCompleto = rankingData.stream()
                .map(row -> {
                    String coIbgeRow = (String) row[0];
                    long totalCasos = ((Number) row[3]).longValue();
                    long populacao = ((Number) row[4]).longValue();
                    
                    double incidencia = populacao > 0 
                            ? (double) totalCasos / populacao * 100_000 
                            : 0;

                    return RankingMunicipioDTO.builder()
                            .coIbge(coIbgeRow)
                            .incidencia100k(incidencia)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k).reversed())
                .collect(Collectors.toList());

        int total = rankingCompleto.size();
        for (int i = 0; i < total; i++) {
            if (coIbge.equals(rankingCompleto.get(i).getCoIbge())) {
                return (i + 1) + " de " + total;
            }
        }

        return null;
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
