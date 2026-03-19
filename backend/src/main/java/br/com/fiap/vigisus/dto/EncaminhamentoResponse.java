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
public class EncaminhamentoResponse {

    private String municipioOrigem;
    private String condicao;
    private String gravidade;
    private List<HospitalDTO> hospitais;
}
