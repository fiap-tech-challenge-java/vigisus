package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Resultado da avaliação de triagem do paciente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriagemResponse {

    @Schema(description = "Prioridade de atendimento", example = "URGENTE")
    private String prioridade;

    @Schema(description = "Cor do protocolo de triagem (Manchester)", example = "LARANJA")
    private String corProtocolo;

    @Schema(description = "Alerta epidemiológico do município", example = "SURTO")
    private String alertaEpidemiologico;

    @Schema(description = "Recomendação clínica ao profissional de saúde")
    private String recomendacao;

    @Schema(description = "Sinais de alarme identificados")
    private List<String> sinaisAlarme;

    @Schema(description = "Indica se o paciente requer observação", example = "true")
    private boolean requerObservacao;

    @Schema(description = "Hospital de encaminhamento sugerido")
    private EncaminhamentoResponse.HospitalDTO encaminhamento;

    @Schema(description = "Texto analítico gerado por IA")
    private String textoIa;
}
