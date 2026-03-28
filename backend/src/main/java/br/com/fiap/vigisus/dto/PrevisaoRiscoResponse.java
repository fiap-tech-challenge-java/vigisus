package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Previsão de risco epidemiológico para o município nos próximos 14 dias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoRiscoResponse {

    @Schema(description = "Código IBGE do município", example = "3509502")
    private String coIbge;

    @Schema(description = "Nome do município", example = "Campinas")
    private String municipio;

    @Schema(description = "Score de risco calculado (0–100)", example = "72")
    private int score;

    @Schema(description = "Sigla da UF do município", example = "SP")
    private String uf;

    @Schema(description = "Temperatura média atual (°C)", example = "30.5")
    private Double temperaturaMedia;

    @Schema(description = "Chuva acumulada (mm)", example = "12.3")
    private Double chuvaAcumulada;

    @Schema(description = "Classificação do risco", example = "ALTO")
    private String classificacao;

    @Schema(description = "Incidência histórica média do município", example = "388.7")
    private Double incidencia;          // Incidência histórica (média de cidades)

    @Schema(description = "Fatores que contribuíram para o score de risco")
    private List<String> fatores;

    @Schema(description = "Texto analítico gerado por IA")
    private String textoIa;

    @Schema(description = "Scores diários de risco para os próximos 14 dias")
    // Daily risk scores for the 14 coloured circles in the front-end
    private List<RiscoDiarioDTO> risco14Dias;
}
