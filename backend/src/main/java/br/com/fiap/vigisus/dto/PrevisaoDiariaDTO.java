package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoDiariaDTO {

    private String data;
    private Double temperaturaMaxima;
    private Double precipitacaoTotal;
    private Integer probabilidadeChuva;
}
