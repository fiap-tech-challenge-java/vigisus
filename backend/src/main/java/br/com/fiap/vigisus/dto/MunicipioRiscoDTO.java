package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MunicipioRiscoDTO {
    private String coIbge;
    private String municipio;
    private String sgUf;
    private long totalCasos;
    private double incidencia;
    private String classificacao;
    private int posicao;  // posição no ranking de piores
}
