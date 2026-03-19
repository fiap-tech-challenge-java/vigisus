package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.exception.RecursoNaoEncontradoException;
import br.com.fiap.vigisus.dto.ComparativoEstadoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerfilEpidemiologicoService {

    private final MunicipioService municipioService;
    private final CasoDengueRepository casoDengueRepository;
    private final RankingService rankingService;

    public PerfilEpidemiologicoResponse gerarPerfil(String coIbge, String doenca, int ano) {
        Municipio municipio = municipioService.buscarPorCoIbge(coIbge);

        long total = casoDengueRepository
                .sumTotalCasosByCoMunicipioAndAno(coIbge, ano);

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
}
