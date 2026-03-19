package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItemDTO {

    private int posicao;
    private String coIbge;
    private String municipio;
    private String uf;
    private long totalCasos;
    private double incidencia;
    private String classificacao;
}
