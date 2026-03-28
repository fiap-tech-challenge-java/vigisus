package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.domain.epidemiologia.CalculadoraTendenciaEpidemiologica;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Camada de orquestração que coordena use cases e integrações externas.
 *
 * <p>Depende exclusivamente de Ports (application/port/) — nunca de repositórios
 * JPA diretamente — respeitando a regra de dependência da Clean Architecture.
 *
 * <p>Candidato à migração para use case dedicado na versão 2.0.
 */
@Service
@RequiredArgsConstructor
public class EstadoHistoricoService {

    private final CasoDenguePort casoDenguePort;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;
    private final CalculadoraTendenciaEpidemiologica calculadoraTendenciaEpidemiologica;

    @Cacheable(value = "estado-historico", key = "#uf + '-' + #doenca + '-' + #ano")
    public PerfilEpidemiologicoResponse gerarPerfilEstado(String uf, String doenca, int ano) {
        String ufUpper = (uf == null ? "" : uf.trim().toUpperCase(Locale.ROOT));

        List<Object[]> totais = casoDenguePort.agregaTotaisEstadoNoAno(ufUpper, ano);
        Object[] row = (totais == null || totais.isEmpty()) ? null : totais.get(0);
        long totalCasos = row != null && row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long populacao = row != null && row[1] != null ? ((Number) row[1]).longValue() : 0L;

        double incidencia = populacao > 0 ? (double) totalCasos / populacao * 100_000 : 0.0;
        String classificacao = classificacaoEpidemiologicaPolicy.classificar(incidencia);

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
                .tendencia(calculadoraTendenciaEpidemiologica.calcular(semanas))
                .semanas(semanas)
                .semanasAnoAnterior(semanasAnoAnterior)
                .build();
    }

    private List<SemanaDTO> agregarSemanasEstado(String uf, int ano) {
        return casoDenguePort.agregaSemanasPorEstado(uf, ano)
                .stream()
                .map(row -> SemanaDTO.builder()
                        .semanaEpi(((Number) row[0]).intValue())
                        .casos(((Number) row[1]).intValue())
                        .build())
                .sorted(Comparator.comparingInt(SemanaDTO::getSemanaEpi))
                .collect(Collectors.toList());
    }

}
