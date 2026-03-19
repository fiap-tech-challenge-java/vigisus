package br.com.fiap.vigisus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriagemRequest {

    @NotBlank
    private String municipio;

    @NotNull
    private List<String> sintomas;

    @Min(0)
    private int diasSintomas;

    @Min(0)
    private int idade;

    @NotNull
    private List<String> comorbidades;
}
