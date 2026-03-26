package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.domain.epidemiologia.CalculadoraTendenciaEpidemiologica;
import br.com.fiap.vigisus.domain.epidemiologia.ClassificacaoEpidemiologicaPolicy;
import br.com.fiap.vigisus.dto.ComparativoEstadoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilEpidemiologicoService {

    private final MunicipioService municipioService;
    private final CasoDenguePort casoDenguePort;
    private final RankingService rankingService;
    private final ClassificacaoEpidemiologicaPolicy classificacaoEpidemiologicaPolicy;
    private final CalculadoraTendenciaEpidemiologica calculadoraTendenciaEpidemiologica;

    @Cacheable(value = "perfil-epidemiologico", key = "#coIbge + '-' + #doenca + '-' + #ano")
    public PerfilEpidemiologicoResponse gerarPerfil(String coIbge, String doenca, int ano) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        long total = casoDenguePort
                .sumTotalCasosByCoMunicipioAndAno(coIbge, ano);

        if (total == 0) {
            throw new DadosInsuficientesException(municipio.getNoMunicipio(), ano);
        }

        long populacao = municipio.getPopulacao() != null && municipio.getPopulacao() > 0
                ? municipio.getPopulacao()
                : 0L;
        if (populacao == 0L) {
            throw new RecursoNaoEncontradoException("Município", coIbge);
        }
        double incidencia = (double) total / populacao * 100_000;

        String classificacao = classificacaoEpidemiologicaPolicy.classificar(incidencia);

        String posicao = rankingService.calcularPosicaoNoEstado(coIbge, municipio.getSgUf(), doenca, ano);
        ComparativoEstadoDTO comparativoEstado = posicao != null
                ? ComparativoEstadoDTO.builder().posicaoRankingEstado(posicao).build()
                : null;

        List<SemanaDTO> semanasAnoAtual = casoDenguePort
                .findByCoMunicipioAndAnoOrderBySemanaEpiAsc(coIbge, ano)
                .stream()
                .map(c -> SemanaDTO.builder()
                        .semanaEpi(c.getSemanaEpi())
                        .casos(c.getTotalCasos() != null ? c.getTotalCasos().intValue() : 0)
                        .build())
                .collect(Collectors.toList());

        String tendencia = calculadoraTendenciaEpidemiologica.calcular(semanasAnoAtual);
        List<SemanaDTO> semanasAnoAnterior = buscarAnoAnterior(coIbge, ano);
        Double incidenciaMediaEstado = calcularMediaEstado(municipio.getSgUf(), ano);

        String posicaoEstado = posicao != null
                ? posicao + " municípios em " + municipio.getSgUf()
                : null;

        return PerfilEpidemiologicoResponse.builder()
                .coIbge(coIbge)
                .municipio(municipio.getNoMunicipio())
                .uf(municipio.getSgUf())
                .doenca(doenca)
                .ano(ano)
                .total(total)
                .incidencia(incidencia)
                .classificacao(classificacao)
                .comparativoEstado(comparativoEstado)
                .tendencia(tendencia)
                .semanas(semanasAnoAtual)
                .semanasAnoAnterior(semanasAnoAnterior)
                .incidenciaMediaEstado(incidenciaMediaEstado)
                .posicaoEstado(posicaoEstado)
                .nuLatitude(municipio.getNuLatitude())
                .nuLongitude(municipio.getNuLongitude())
                .build();
    }

    private List<SemanaDTO> buscarAnoAnterior(String coMunicipio, int ano) {
        return casoDenguePort
                .findByCoMunicipioAndAnoOrderBySemanaEpiAsc(coMunicipio, ano - 1)
                .stream()
                .map(c -> SemanaDTO.builder()
                        .semanaEpi(c.getSemanaEpi())
                        .casos(c.getTotalCasos() != null ? c.getTotalCasos().intValue() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    private Double calcularMediaEstado(String sgUf, int ano) {
        List<Municipio> municipiosUf = municipioService.listarPorUf(sgUf);

        List<Double> incidencias = new ArrayList<>();
        for (Municipio m : municipiosUf) {
            if (m.getPopulacao() == null || m.getPopulacao() == 0) continue;
            long totalCasos = casoDenguePort
                    .sumTotalCasosByCoMunicipioAndAno(m.getCoIbge(), ano);
            if (totalCasos > 0) {
                incidencias.add((double) totalCasos / m.getPopulacao() * 100_000);
            }
        }

        if (incidencias.isEmpty()) return null;
        return incidencias.stream().mapToDouble(d -> d).average().orElse(0);
    }
}
