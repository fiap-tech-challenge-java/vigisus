package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.ComparativoEstadoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
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
    private final CasoDengueRepository casoDengueRepository;
    private final RankingService rankingService;

    @Cacheable(value = "perfil-epidemiologico", key = "#coIbge + '-' + #doenca + '-' + #ano")
    public PerfilEpidemiologicoResponse gerarPerfil(String coIbge, String doenca, int ano) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        long total = casoDengueRepository
                .sumTotalCasosByCoMunicipioAndAno(coIbge, ano);

        if (total == 0) {
            throw new DadosInsuficientesException(municipio.getNoMunicipio(), ano);
        }

        long populacao = municipio.getPopulacao() != null && municipio.getPopulacao() > 0
                ? municipio.getPopulacao()
                : 0L;
        if (populacao == 0L) {
            throw new RecursoNaoEncontradoException(
                    "População não disponível para o município: " + coIbge);
        }
        double incidencia = (double) total / populacao * 100_000;

        String classificacao = classificar(incidencia);

        String posicao = rankingService.calcularPosicaoNoEstado(coIbge, municipio.getSgUf(), doenca, ano);
        ComparativoEstadoDTO comparativoEstado = posicao != null
                ? ComparativoEstadoDTO.builder().posicaoRankingEstado(posicao).build()
                : null;

        List<SemanaDTO> semanasAnoAtual = casoDengueRepository
                .findByCoMunicipioAndAnoOrderBySemanaEpiAsc(coIbge, ano)
                .stream()
                .map(c -> SemanaDTO.builder()
                        .semanaEpi(c.getSemanaEpi())
                        .casos(c.getTotalCasos() != null ? c.getTotalCasos().intValue() : 0)
                        .build())
                .collect(Collectors.toList());

        String tendencia = calcularTendencia(semanasAnoAtual);
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

    private String classificar(double incidencia) {
        if (incidencia < 50) {
            return "BAIXO";
        } else if (incidencia < 100) {
            return "MODERADO";
        } else if (incidencia <= 300) {
            return "ALTO";
        } else {
            return "EPIDEMIA";
        }
    }

    private String calcularTendencia(List<SemanaDTO> semanas) {
        if (semanas == null || semanas.size() < 8) return "ESTAVEL";

        List<SemanaDTO> comDados = semanas.stream()
                .filter(s -> s.getCasos() > 0)
                .collect(Collectors.toList());

        if (comDados.size() < 8) return "ESTAVEL";

        int n = comDados.size();
        int somaUltimas4 = comDados.subList(n - 4, n).stream()
                .mapToInt(SemanaDTO::getCasos).sum();
        int somaAnteriores4 = comDados.subList(n - 8, n - 4).stream()
                .mapToInt(SemanaDTO::getCasos).sum();

        if (somaAnteriores4 == 0) return "ESTAVEL";

        double variacao = (double) (somaUltimas4 - somaAnteriores4) / somaAnteriores4;

        if (variacao > 0.2) return "CRESCENTE";
        if (variacao < -0.2) return "DECRESCENTE";
        return "ESTAVEL";
    }

    private List<SemanaDTO> buscarAnoAnterior(String coMunicipio, int ano) {
        return casoDengueRepository
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
            long totalCasos = casoDengueRepository
                    .sumTotalCasosByCoMunicipioAndAno(m.getCoIbge(), ano);
            if (totalCasos > 0) {
                incidencias.add((double) totalCasos / m.getPopulacao() * 100_000);
            }
        }

        if (incidencias.isEmpty()) return null;
        return incidencias.stream().mapToDouble(d -> d).average().orElse(0);
    }
}
