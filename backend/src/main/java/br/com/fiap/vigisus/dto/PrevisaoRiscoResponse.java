package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoRiscoResponse {

    private String coIbge;
    private String nomeMunicipio;
    private String doenca;
    private String nivelRisco;
    private Double scoreRisco;
    private String textoIa;
}
