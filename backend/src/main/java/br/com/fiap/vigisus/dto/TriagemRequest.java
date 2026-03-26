package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Dados do paciente para avaliação de triagem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriagemRequest {

    @Schema(description = "Nome do município do atendimento", example = "Campinas")
    @NotBlank
    private String municipio;

    @Schema(description = "Lista de sintomas apresentados pelo paciente",
            example = "[\"FEBRE_ALTA\", \"DOR_CABECA\", \"MIALGIA\"]")
    @NotNull
    private List<String> sintomas;

    @Schema(description = "Número de dias desde o início dos sintomas", example = "3")
    @Min(0)
    private int diasSintomas;

    @Schema(description = "Idade do paciente em anos", example = "35")
    @Min(0)
    private int idade;

    @Schema(description = "Lista de comorbidades do paciente", example = "[\"DIABETES\", \"HIPERTENSAO\"]")
    @NotNull
    private List<String> comorbidades;
}
