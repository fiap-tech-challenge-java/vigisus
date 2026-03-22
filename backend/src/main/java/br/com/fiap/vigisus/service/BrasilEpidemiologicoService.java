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
        // 1. Buscar todos os municípios
        List<Municipio> todosMunicipios = municipioRepository.findAll();
        
        // 2. Calcular totais agregados por município
        Map<String, Long> casosPerMunicipio = new HashMap<>();
        Map<String, Municipio> municipiosMap = new HashMap<>();
        
        for (Municipio m : todosMunicipios) {
            long total = casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(m.getCoIbge(), ano);
            if (total > 0) {
                casosPerMunicipio.put(m.getCoIbge(), total);
                municipiosMap.put(m.getCoIbge(), m);
            }
        }
        
        if (casosPerMunicipio.isEmpty()) {
            throw new DadosInsuficientesException("Brasil", ano);
        }
        
        // 3. Calcular total geral e agregações
        long totalCasos = casosPerMunicipio.values().stream()
                .mapToLong(Long::longValue).sum();
        
        // População total do Brasil
        long populacaoTotal = todosMunicipios.stream()
                .mapToLong(m -> m.getPopulacao() != null ? m.getPopulacao() : 0)
                .sum();
        
        double incidencia = populacaoTotal > 0 
                ? (double) totalCasos / populacaoTotal * 100_000 
                : 0;
        
        String classificacao = classificar(incidencia);
        
        // 4. Agregar semanas epidemiológicas (Brasil todo)
        List<SemanaDTO> semanasAnoAtual = agregarSemanasBrasil(ano);
        String tendencia = calcularTendencia(semanasAnoAtual);
        List<SemanaDTO> semanasAnoAnterior = agregarSemanasBrasil(ano - 1);
        
        // 5. Agregar por estado (para top 5 piores)
        Map<String, Long> casosPerEstado = new HashMap<>();
        Map<String, Long> populacaoPerEstado = new HashMap<>();
        
        for (Municipio m : todosMunicipios) {
            String uf = m.getSgUf();
            long casos = casosPerMunicipio.getOrDefault(m.getCoIbge(), 0L);
            long pop = m.getPopulacao() != null ? m.getPopulacao() : 0;
            
            casosPerEstado.put(uf, casosPerEstado.getOrDefault(uf, 0L) + casos);
            populacaoPerEstado.put(uf, populacaoPerEstado.getOrDefault(uf, 0L) + pop);
        }
        
        List<EstadoDTO> estadosPiores = casosPerEstado.entrySet().stream()
                .map(entry -> {
                    String uf = entry.getKey();
                    long casos = entry.getValue();
                    long populacao = populacaoPerEstado.getOrDefault(uf, 0L);
                    double incidenciaEstado = populacao > 0 
                            ? (double) casos / populacao * 100_000 
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
        
        // Adicionar posição
        for (int i = 0; i < estadosPiores.size(); i++) {
            estadosPiores.get(i).setPosicao(i + 1);
        }
        
        // 6. Top 5 piores municípios do Brasil
        List<MunicipioRiscoDTO> municipiosPiores = casosPerMunicipio.entrySet().stream()
                .map(entry -> {
                    String coIbge = entry.getKey();
                    long casos = entry.getValue();
                    Municipio m = municipiosMap.get(coIbge);
                    long populacao = m.getPopulacao() != null ? m.getPopulacao() : 0;
                    double incidenciaMun = populacao > 0 
                            ? (double) casos / populacao * 100_000 
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
                .sorted((a, b) -> Double.compare(b.getIncidencia(), a.getIncidencia()))
                .limit(5)
                .collect(Collectors.toList());
        
        // Adicionar posição
        for (int i = 0; i < municipiosPiores.size(); i++) {
            municipiosPiores.get(i).setPosicao(i + 1);
        }
        
        // 7. Buscar principais hospitais das capitais
        var hospitais = encaminhamentoService.buscarHospitaisDasCapitais(null);
        
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
                .hospitais(hospitais)
                .build();
    }
    
    private List<SemanaDTO> agregarSemanasBrasil(int ano) {
        // Em vez de carregar tudo, podemos calcular a agregação direto dos municipios
        // que já temos casos Vamos aproveitar que municipioRepository existe
        List<Municipio> municipios = municipioRepository.findAll();
        Map<Integer, Long> casosPorSemana = new HashMap<>();
        
        for (int semana = 1; semana <= 53; semana++) {
            long totalSemana = 0;
            for (Municipio m : municipios) {
                long casos = casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(m.getCoIbge(), ano);
                if (casos > 0) {
                    totalSemana += casos; // simplificado: não há agregação por semana no banco
                }
            }
            if (totalSemana > 0) {
                casosPorSemana.put(semana, totalSemana);
            }
        }
        
        return casosPorSemana.entrySet().stream()
                .map(entry -> SemanaDTO.builder()
                        .semanaEpi(entry.getKey())
                        .casos(entry.getValue().intValue())
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
