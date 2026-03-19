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
public class TriagemResponse {

    private String prioridade;
    private String corProtocolo;
    private String alertaEpidemiologico;
    private String recomendacao;
    private List<String> sinaisAlarme;
    private boolean requerObservacao;
    private EncaminhamentoResponse.HospitalDTO encaminhamento;
    private String textoIa;
}
