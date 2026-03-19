package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscaResponse {

    private IntencaoDTO interpretacao;
    private PerfilEpidemiologicoResponse dados;
    private String textoIa;
}
