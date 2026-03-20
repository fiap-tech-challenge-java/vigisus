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
public class PerfilEpidemiologicoResponse {

    private String coIbge;
    private String municipio;
    private String uf;
    private String doenca;
    private int ano;
    private long total;
    private double incidencia;
    private String classificacao;
    private String textoIa;
    private ComparativoEstadoDTO comparativoEstado;

    // Trend calculated by comparing last 4 weeks vs previous 4 weeks
    private String tendencia;

    // Previous year's weekly data for comparative chart
    private List<SemanaDTO> semanasAnoAnterior;

    // Average incidence across other municipalities in the same state
    private Double incidenciaMediaEstado;

    // Position among state municipalities (e.g. "47º de 853 municípios em MG")
    private String posicaoEstado;
}
