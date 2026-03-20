package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiscoDiarioDTO {

    private String data;
    private int scoreDia;
    private String classificacao;
    private double tempMax;
    private double chuvaMm;
    private double probChuva;
}
