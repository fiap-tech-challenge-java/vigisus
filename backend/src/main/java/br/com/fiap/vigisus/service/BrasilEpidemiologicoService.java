package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.domain.epidemiologia.CalculadoraTendenciaEpidemiologica;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.domain.geografia.CatalogoGeograficoBrasil;
import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.EstadoDTO;
import br.com.fiap.vigisus.dto.MunicipioRiscoDTO;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrasilEpidemiologicoService {

    private final MunicipioPort municipioPort;
    private final CasoDenguePort casoDenguePort;
    private final EncaminhamentoService encaminhamentoService;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;
    private final CalculadoraTendenciaEpidemiologica calculadoraTendenciaEpidemiologica;
    private final CatalogoGeograficoBrasil catalogoGeograficoBrasil;

    @Cacheable(value = "brasil-epidemiologico", key = "#doenca + '-' + #ano")
    public BrasilEpidemiologicoResponse gerarPerfilBrasil(String doenca, int ano) {
        List<Object[]> casosPorEstado = casoDenguePort.agregaCasosPorEstadoNoAno(ano);
        if (casosPorEstado.isEmpty()) {
            throw new DadosInsuficientesException("Brasil", ano);
        }

        long totalCasos = 0;
        long populacaoTotal = 0;
        for (Object[] row : casosPorEstado) {
            totalCasos += ((Number) row[1]).longValue();
            populacaoTotal += ((Number) row[2]).longValue();
        }

        double incidencia = populacaoTotal > 0
                ? (double) totalCasos / populacaoTotal * 100_000
                : 0;
        String classificacao = classificacaoEpidemiologicaPolicy.classificar(incidencia);

        List<SemanaDTO> semanasAnoAtual = agregarSemanasBrasilOtimizado(ano);
        String tendencia = calculadoraTendenciaEpidemiologica.calcular(semanasAnoAtual);
        List<SemanaDTO> semanasAnoAnterior = agregarSemanasBrasilOtimizado(ano - 1);

        List<EstadoDTO> estadosPiores = casosPorEstado.stream()
                .map(row -> {
                    String uf = (String) row[0];
                    long casos = ((Number) row[1]).longValue();
                    long populacao = ((Number) row[2]).longValue();
                    double incidenciaEstado = populacao > 0
                            ? (double) casos / populacao * 100_000
                            : 0;
                    return EstadoDTO.builder()
                            .sgUf(uf)
                            .nome(catalogoGeograficoBrasil.nomeEstado(uf))
                            .totalCasos(casos)
                            .incidencia(incidenciaEstado)
                            .classificacao(classificacaoEpidemiologicaPolicy.classificar(incidenciaEstado))
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getIncidencia(), a.getIncidencia()))
                .limit(5)
                .collect(Collectors.toList());

        for (int i = 0; i < estadosPiores.size(); i++) {
            estadosPiores.get(i).setPosicao(i + 1);
        }

        List<Object[]> municipiosCasos = casoDenguePort.agregaCasosPorMunicipioNoAno(ano);
        List<MunicipioRiscoDTO> municipiosPiores = municipiosCasos.stream()
                .map(row -> {
                    String coIbge = (String) row[0];
                    long casos = ((Number) row[1]).longValue();
                    return municipioPort.findByCoIbge(coIbge)
                            .map(municipio -> mapearMunicipioRisco(coIbge, casos, municipio))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getIncidencia(), a.getIncidencia()))
                .limit(5)
                .collect(Collectors.toList());

        for (int i = 0; i < municipiosPiores.size(); i++) {
            municipiosPiores.get(i).setPosicao(i + 1);
        }

        Map<String, Long> casosPerEstado = casosPorEstado.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        return BrasilEpidemiologicoResponse.builder()
                .regiao("Brasil")
                .doenca(doenca)
                .ano(ano)
                .totalCasos(totalCasos)
                .incidencia(incidencia)
                .classificacao(classificacao)
                .tendencia(tendencia)
                .semanas(semanasAnoAtual)
                .semanasAnoAnterior(semanasAnoAnterior)
                .estadosPiores(estadosPiores)
                .municipiosPiores(municipiosPiores)
                .casosPerEstado(casosPerEstado)
                .hospitais(encaminhamentoService.buscarHospitaisDasCapitais(null))
                .build();
    }

    private MunicipioRiscoDTO mapearMunicipioRisco(String coIbge, long casos, Municipio municipio) {
        long populacao = municipio.getPopulacao() != null ? municipio.getPopulacao() : 0;
        double incidencia = populacao > 0 ? (double) casos / populacao * 100_000 : 0;

        return MunicipioRiscoDTO.builder()
                .coIbge(coIbge)
                .municipio(municipio.getNoMunicipio())
                .sgUf(municipio.getSgUf())
                .totalCasos(casos)
                .incidencia(incidencia)
                .classificacao(classificacaoEpidemiologicaPolicy.classificar(incidencia))
                .build();
    }

    private List<SemanaDTO> agregarSemanasBrasilOtimizado(int ano) {
        return casoDenguePort.agregaSemanasBrasil(ano).stream()
                .map(row -> SemanaDTO.builder()
                        .semanaEpi(((Number) row[0]).intValue())
                        .casos(((Number) row[1]).intValue())
                        .build())
                .sorted(Comparator.comparingInt(SemanaDTO::getSemanaEpi))
                .collect(Collectors.toList());
    }
}
