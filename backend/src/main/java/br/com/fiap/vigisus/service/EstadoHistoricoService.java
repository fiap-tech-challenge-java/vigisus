package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadoHistoricoService {

    private final CasoDengueRepository casoDengueRepository;

    @Cacheable(value = "estado-historico", key = "#uf + '-' + #doenca + '-' + #ano")
    public PerfilEpidemiologicoResponse gerarPerfilEstado(String uf, String doenca, int ano) {
        String ufUpper = (uf == null ? "" : uf.trim().toUpperCase(Locale.ROOT));

        List<Object[]> totais = casoDengueRepository.agregaTotaisEstadoNoAno(ufUpper, ano);
        Object[] row = (totais == null || totais.isEmpty()) ? null : totais.get(0);
        long totalCasos = row != null && row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long populacao = row != null && row[1] != null ? ((Number) row[1]).longValue() : 0L;

        double incidencia = populacao > 0 ? (double) totalCasos / populacao * 100_000 : 0.0;
        String classificacao = classificar(incidencia);

        List<SemanaDTO> semanas = agregarSemanasEstado(ufUpper, ano);
        List<SemanaDTO> semanasAnoAnterior = agregarSemanasEstado(ufUpper, ano - 1);

        return PerfilEpidemiologicoResponse.builder()
                .coIbge(ufUpper)
                .municipio("Estado " + ufUpper)
                .uf(ufUpper)
                .doenca(doenca)
                .ano(ano)
                .total(totalCasos)
                .incidencia(incidencia)
                .classificacao(classificacao)
                .tendencia(calcularTendencia(semanas))
                .semanas(semanas)
                .semanasAnoAnterior(semanasAnoAnterior)
                .build();
    }

    private List<SemanaDTO> agregarSemanasEstado(String uf, int ano) {
        return casoDengueRepository.agregaSemanasPorEstado(uf, ano)
                .stream()
                .map(row -> SemanaDTO.builder()
                        .semanaEpi(((Number) row[0]).intValue())
                        .casos(((Number) row[1]).intValue())
                        .build())
                .sorted(Comparator.comparingInt(SemanaDTO::getSemanaEpi))
                .collect(Collectors.toList());
    }

    private String calcularTendencia(List<SemanaDTO> semanas) {
        if (semanas == null || semanas.size() < 8) {
            return "ESTAVEL";
        }

        List<SemanaDTO> comDados = semanas.stream()
                .filter(s -> s.getCasos() > 0)
                .collect(Collectors.toList());

        if (comDados.size() < 8) {
            return "ESTAVEL";
        }

        int n = comDados.size();
        long somaUltimas4 = comDados.subList(n - 4, n).stream()
                .mapToLong(SemanaDTO::getCasos)
                .sum();
        long somaAnteriores4 = comDados.subList(n - 8, n - 4).stream()
                .mapToLong(SemanaDTO::getCasos)
                .sum();

        if (somaAnteriores4 == 0) {
            return "ESTAVEL";
        }

        double variacao = (double) (somaUltimas4 - somaAnteriores4) / somaAnteriores4;

        if (variacao > 0.2) {
            return "CRESCENTE";
        }
        if (variacao < -0.2) {
            return "DECRESCENTE";
        }
        return "ESTAVEL";
    }

    private String classificar(double incidencia) {
        if (incidencia < 50) {
            return "BAIXO";
        }
        if (incidencia < 100) {
            return "MODERADO";
        }
        if (incidencia <= 300) {
            return "ALTO";
        }
        return "EPIDEMIA";
    }
}
