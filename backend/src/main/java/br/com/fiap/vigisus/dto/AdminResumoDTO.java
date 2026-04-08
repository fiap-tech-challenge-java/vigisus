package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResumoDTO {

    private long totalCasos;
    private double incidenciaNacional;
    private String classificacaoNacional;
    private String tendencia;
    private int totalMunicipiosComDados;
    private int totalEstadosAfetados;
    private int municipiosAltoRisco;
    private int municipiosEpidemia;
    private String doenca;
    private int ano;
}
