package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilEpidemiologicoResponse {

    private String coIbge;
    private String municipio;
    private String uf;
    private String doenca;
    private int ano;
    private long total;
    private double incidencia;
    private String classificacao;
    private String textoIa;
}
