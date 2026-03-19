package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoRiscoResponse {

    private String coIbge;
    private String municipio;
    private int score;
    private String classificacao;
    private List<String> fatores;
    private String textoIa;
}
