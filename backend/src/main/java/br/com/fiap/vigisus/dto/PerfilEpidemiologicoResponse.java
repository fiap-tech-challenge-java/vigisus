package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Perfil epidemiológico de um município com histórico de casos por semana")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilEpidemiologicoResponse {

    @Schema(description = "Código IBGE do município", example = "3509502")
    private String coIbge;

    @Schema(description = "Nome do município", example = "Campinas")
    private String municipio;

    @Schema(description = "Sigla do estado", example = "SP")
    private String uf;

    @Schema(description = "Doença analisada", example = "DENGUE")
    private String doenca;

    @Schema(description = "Ano de referência dos dados", example = "2024")
    private int ano;

    @Schema(description = "Total de casos notificados no período", example = "4523")
    private long total;

    @Schema(description = "Incidência por 100 mil habitantes", example = "388.7")
    private double incidencia;

    @Schema(description = "Classificação epidemiológica", example = "EPIDEMIA")
    private String classificacao;

    @Schema(description = "Texto analítico gerado por IA")
    private String textoIa;

    private ComparativoEstadoDTO comparativoEstado;

    @Schema(description = "Tendência dos casos nas últimas semanas", example = "CRESCENTE")
    // Trend calculated by comparing last 4 weeks vs previous 4 weeks
    private String tendencia;

    @Schema(description = "Dados semanais do ano corrente")
    // Current year's weekly data (semana epidemiológica → casos)
    private List<SemanaDTO> semanas;

    @Schema(description = "Dados semanais do ano anterior para comparativo")
    // Previous year's weekly data for comparative chart
    private List<SemanaDTO> semanasAnoAnterior;

    @Schema(description = "Incidência média dos municípios do mesmo estado", example = "210.4")
    // Average incidence across other municipalities in the same state
    private Double incidenciaMediaEstado;

    @Schema(description = "Posição no ranking estadual", example = "47º de 853 municípios em SP")
    // Position among state municipalities (e.g. "47º de 853 municípios em MG")
    private String posicaoEstado;

    @Schema(description = "Latitude geográfica do município", example = "-22.9140")
    // Geographic coordinates of the municipality for map centering
    private Double nuLatitude;

    @Schema(description = "Longitude geográfica do município", example = "-47.0628")
    private Double nuLongitude;
}
