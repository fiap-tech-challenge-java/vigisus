package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingMunicipioDTO {

    private int posicao;
    private String coIbge;
    private String municipio;
    private long totalCasos;
    private long populacao;
    private double incidencia100k;
    private String classificacao;
}
