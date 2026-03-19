package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PressaoOperacionalRequest {

    private String municipio;
    private int suspeitasDengueDia;
    private String tipoUnidade;
}
