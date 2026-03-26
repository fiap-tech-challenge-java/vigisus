package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Parâmetros para análise de pressão operacional da unidade de saúde")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PressaoOperacionalRequest {

    @Schema(description = "Nome do município para análise", example = "Campinas")
    private String municipio;

    @Schema(description = "Número estimado de suspeitas de dengue por dia na unidade", example = "15")
    private int suspeitasDengueDia;

    @Schema(description = "Tipo da unidade de saúde", example = "UBS")
    private String tipoUnidade;
}
