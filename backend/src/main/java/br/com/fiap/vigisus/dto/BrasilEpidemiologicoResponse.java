package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrasilEpidemiologicoResponse {

    // Brasil agregado
    private String regiao;  // "Brasil"
    private String doenca;
    private int ano;
    private long totalCasos;
    private double incidencia;
    private String classificacao;
    private String textoIa;
    
    // Semanas epidemiológicas agregadas
    private List<SemanaDTO> semanas;
    private List<SemanaDTO> semanasAnoAnterior;
    
    // Tendência geral
    private String tendencia;
    
    // Top 5 piores estados
    private List<EstadoDTO> estadosPiores;
    
    // Top 5 piores municípios no Brasil
    private List<MunicipioRiscoDTO> municipiosPiores;
    
    // Agregação por semana e por estado (para visualizações)
    private Map<String, Long> casosPerEstado;
    
    // Principais hospitais das capitais
    private List<EncaminhamentoResponse.HospitalDTO> hospitais;
}
