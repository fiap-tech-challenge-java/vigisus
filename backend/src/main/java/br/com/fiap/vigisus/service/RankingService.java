package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.dto.RankingMunicipioDTO;
import br.com.fiap.vigisus.dto.RankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int MAX_RANKING_LIMIT = 1000;

    private final CasoDenguePort casoDenguePort;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;

    @Cacheable(value = "ranking-municipal", key = "#uf + '-' + #doenca + '-' + #ano")
    public RankingResponse calcularRanking(String uf, String doenca, int ano, int top, String ordem) {
        int topLimitado = Math.min(top, MAX_RANKING_LIMIT);
        List<Object[]> rankingData = casoDenguePort.rankingOtimizadoPorEstado(uf.toUpperCase(), ano);

        List<RankingMunicipioDTO> rankingCompleto = rankingData.stream()
                .map(this::mapearRankingMunicipio)
                .filter(dto -> dto.getPopulacao() > 0)
                .collect(Collectors.toList());

        Comparator<RankingMunicipioDTO> comparator = Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k);
        if ("melhores".equalsIgnoreCase(ordem)) {
            rankingCompleto.sort(comparator);
        } else {
            rankingCompleto.sort(comparator.reversed());
        }

        for (int i = 0; i < rankingCompleto.size(); i++) {
            rankingCompleto.get(i).setPosicao(i + 1);
        }

        return RankingResponse.builder()
                .uf(uf.toUpperCase())
                .doenca(doenca)
                .ano(ano)
                .totalMunicipiosComDados(rankingCompleto.size())
                .ranking(rankingCompleto.stream().limit(topLimitado).collect(Collectors.toList()))
                .build();
    }

    public String calcularPosicaoNoEstado(String coIbge, String uf, String doenca, int ano) {
        List<Object[]> rankingData = casoDenguePort.rankingOtimizadoPorEstado(uf.toUpperCase(), ano);

        List<RankingMunicipioDTO> rankingCompleto = rankingData.stream()
                .map(this::mapearRankingMunicipio)
                .filter(dto -> dto.getPopulacao() > 0)
                .sorted(Comparator.comparingDouble(RankingMunicipioDTO::getIncidencia100k).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < rankingCompleto.size(); i++) {
            if (coIbge.equals(rankingCompleto.get(i).getCoIbge())) {
                return (i + 1) + " de " + rankingCompleto.size();
            }
        }

        return null;
    }

    private RankingMunicipioDTO mapearRankingMunicipio(Object[] row) {
        String coIbge = (String) row[0];
        String municipio = (String) row[1];
        long totalCasos = ((Number) row[3]).longValue();
        long populacao = row[4] instanceof Number number ? number.longValue() : 0L;
        double incidencia = populacao > 0 ? (double) totalCasos / populacao * 100_000 : 0.0;

        return RankingMunicipioDTO.builder()
                .coIbge(coIbge)
                .municipio(municipio)
                .totalCasos(totalCasos)
                .populacao(populacao)
                .incidencia100k(incidencia)
                .classificacao(classificacaoEpidemiologicaPolicy.classificar(incidencia))
                .build();
    }
}
