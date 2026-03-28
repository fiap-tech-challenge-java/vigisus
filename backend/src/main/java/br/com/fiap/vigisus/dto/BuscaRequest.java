package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Busca por linguagem natural sobre situação epidemiológica")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscaRequest {

    @NotBlank(message = "A pergunta não pode estar vazia")
    @Size(min = 3, max = 500, message = "A pergunta deve ter entre 3 e 500 caracteres")
    @Schema(description = "Pergunta em linguagem natural sobre a situação epidemiológica",
            example = "Qual a situação da dengue em Campinas este ano?")
    private String pergunta;
}
