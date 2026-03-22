package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDTO {
    private String sgUf;
    private String nome;
    private long totalCasos;
    private double incidencia;
    private String classificacao;
    private int posicao;  // posição no ranking de piores
}
