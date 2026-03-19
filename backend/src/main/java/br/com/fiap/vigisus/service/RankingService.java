package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.RankingItemDTO;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final MunicipioRepository municipioRepository;
    private final CasoDengueRepository casoDengueRepository;

    @Cacheable(value = "ranking-municipal", key = "#uf + '-' + #doenca + '-' + #ano")
    public List<RankingItemDTO> calcularRanking(String uf, String doenca, int ano, int top, String ordem) {
        List<Municipio> municipios = municipioRepository.findBySgUf(uf.toUpperCase());

        Comparator<RankingItemDTO> comparador = "asc".equalsIgnoreCase(ordem)
                ? Comparator.comparingDouble(RankingItemDTO::getIncidencia)
                : Comparator.comparingDouble(RankingItemDTO::getIncidencia).reversed();

        Stream<RankingItemDTO> stream = municipios.stream()
                .filter(m -> m.getPopulacao() != null && m.getPopulacao() > 0)
                .map(m -> {
                    long total = casoDengueRepository
                            .sumTotalCasosByCoMunicipioAndAno(m.getCoIbge(), ano);
                    double incidencia = (double) total / m.getPopulacao() * 100_000;
                    return RankingItemDTO.builder()
                            .coIbge(m.getCoIbge())
                            .municipio(m.getNoMunicipio())
                            .uf(m.getSgUf())
                            .totalCasos(total)
                            .incidencia(incidencia)
                            .classificacao(classificar(incidencia))
                            .build();
                })
                .sorted(comparador);

        if (top > 0) {
            stream = stream.limit(top);
        }

        List<RankingItemDTO> resultado = stream.toList();

        AtomicInteger posicao = new AtomicInteger(1);
        resultado.forEach(item -> item.setPosicao(posicao.getAndIncrement()));

        return resultado;
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
}
