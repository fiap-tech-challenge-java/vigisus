package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscaCompletaResponse {

    private IntencaoDTO interpretacao;
    private PerfilEpidemiologicoResponse perfil;
    private PrevisaoRiscoResponse risco;
    private EncaminhamentoResponse encaminhamento;
    private String textoIa;
}
