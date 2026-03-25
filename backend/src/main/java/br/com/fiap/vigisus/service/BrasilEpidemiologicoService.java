package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.BrasilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.EstadoDTO;
import br.com.fiap.vigisus.dto.MunicipioRiscoDTO;
import br.com.fiap.vigisus.dto.SemanaDTO;
import br.com.fiap.vigisus.exception.DadosInsuficientesException;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrasilEpidemiologicoService {

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;
    private final EncaminhamentoService encaminhamentoService;

    @Cacheable(value = "brasil-epidemiologico", key = "#doenca + '-' + #ano")
    public BrasilEpidemiologicoResponse gerarPerfilBrasil(String doenca, int ano) {
        // ─────────────────────────────────────────────────────────────
        // OTIMIZAÇÃO: Uma ÚNICA query agregada em vez de N+1
        // ─────────────────────────────────────────────────────────────
        List<Object[]> casosPerEstado = casoDengueRepository.agregaCasosPorEstadoNoAno(ano);
        
        if (casosPerEstado.isEmpty()) {
            throw new DadosInsuficientesException("Brasil", ano);
        }
        
        // 1. Calcular totais do Brasil
        long totalCasos = 0;
        long populacaoTotal = 0;
        for (Object[] row : casosPerEstado) {
            totalCasos += ((Number) row[1]).longValue();
            populacaoTotal += ((Number) row[2]).longValue();
        }
        
        double incidencia = populacaoTotal > 0 
                ? (double) totalCasos / populacaoTotal * 100_000 
                : 0;
        
        String classificacao = classificar(incidencia);
        
        // 2. Agregar semanas epidemiológicas (Brasil todo) - uma query
        List<SemanaDTO> semanasAnoAtual = agregarSemanasBrasilOtimizado(ano);
        String tendencia = calcularTendencia(semanasAnoAtual);
        List<SemanaDTO> semanasAnoAnterior = agregarSemanasBrasilOtimizado(ano - 1);
        
        // 3. Estados piores
        List<EstadoDTO> estadosPiores = casosPerEstado.stream()
                .map(row -> {
                    String uf = (String) row[0];
                    long casos = ((Number) row[1]).longValue();
                    long pop = ((Number) row[2]).longValue();
                    double incidenciaEstado = pop > 0 
                            ? (double) casos / pop * 100_000 
                            : 0;
                    return EstadoDTO.builder()
                            .sgUf(uf)
                            .nome(obterNomeEstado(uf))
                            .totalCasos(casos)
                            .incidencia(incidenciaEstado)
                            .classificacao(classificar(incidenciaEstado))
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getIncidencia(), a.getIncidencia()))
                .limit(5)
                .collect(Collectors.toList());
        
        for (int i = 0; i < estadosPiores.size(); i++) {
            estadosPiores.get(i).setPosicao(i + 1);
        }
        
        // 4. Municípios piores (agregação por município uma query)
        List<Object[]> municípiosCasos = casoDengueRepository.agregaCasosPorMunicipioNoAno(ano);
        
        List<MunicipioRiscoDTO> municipiosPiores = municípiosCasos.stream()
                .map(row -> {
                    String coIbge = (String) row[0];
                    long casos = ((Number) row[1]).longValue();
                    
                    // Buscar município
                    return municipioRepository.findByCoIbge(coIbge)
                            .map(m -> {
                                long pop = m.getPopulacao() != null ? m.getPopulacao() : 0;
                                double incidenciaMun = pop > 0 
                                        ? (double) casos / pop * 100_000 
                                        : 0;
                                return MunicipioRiscoDTO.builder()
                                        .coIbge(coIbge)
                                        .municipio(m.getNoMunicipio())
                                        .sgUf(m.getSgUf())
                                        .totalCasos(casos)
                                        .incidencia(incidenciaMun)
                                        .classificacao(classificar(incidenciaMun))
                                        .build();
                            })
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getIncidencia(), a.getIncidencia()))
                .limit(5)
                .collect(Collectors.toList());
        
        for (int i = 0; i < municipiosPiores.size(); i++) {
            municipiosPiores.get(i).setPosicao(i + 1);
        }
        
        // 5. Hospitais das capitais
        var hospitais = encaminhamentoService.buscarHospitaisDasCapitais(null);
        
        // 6. Mapa de casos por estado para resposta
        Map<String, Long> casosPerEstadoMap = casosPerEstado.stream()
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
                .casosPerEstado(casosPerEstadoMap)
                .hospitais(hospitais)
                .build();
    }
    
    private List<SemanaDTO> agregarSemanasBrasilOtimizado(int ano) {
        // ─────────────────────────────────────────────────────────────
        // OTIMIZAÇÃO: Uma ÚNICA query agregada em vez de N*52 queries
        // ─────────────────────────────────────────────────────────────
        List<Object[]> semanasData = casoDengueRepository.agregaSemanasBrasil(ano);
        
        return semanasData.stream()
                .map(row -> SemanaDTO.builder()
                        .semanaEpi(((Number) row[0]).intValue())
                        .casos(((Number) row[1]).intValue())
                        .build())
                .sorted(Comparator.comparingInt(SemanaDTO::getSemanaEpi))
                .collect(Collectors.toList());
    }
    
    private String calcularTendencia(List<SemanaDTO> semanas) {
        if (semanas == null || semanas.size() < 8) return "ESTAVEL";

        List<SemanaDTO> comDados = semanas.stream()
                .filter(s -> s.getCasos() > 0)
                .collect(Collectors.toList());

        if (comDados.size() < 8) return "ESTAVEL";

        int n = comDados.size();
        long somaUltimas4 = comDados.subList(n - 4, n).stream()
                .mapToLong(SemanaDTO::getCasos).sum();
        long somaAnteriores4 = comDados.subList(n - 8, n - 4).stream()
                .mapToLong(SemanaDTO::getCasos).sum();

        if (somaAnteriores4 == 0) return "ESTAVEL";

        double variacao = (double) (somaUltimas4 - somaAnteriores4) / somaAnteriores4;

        if (variacao > 0.2) return "CRESCENTE";
        if (variacao < -0.2) return "DECRESCENTE";
        return "ESTAVEL";
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
}
