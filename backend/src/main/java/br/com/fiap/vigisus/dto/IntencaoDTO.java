package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntencaoDTO {

    private String municipio;
    private String uf;
    private String doenca;
    private Integer ano;
}
