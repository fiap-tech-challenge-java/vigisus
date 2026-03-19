package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilEpidemiologicoResponse {

    private String coIbge;
    private String nomeMunicipio;
    private String sgUf;
    private String doenca;
    private Integer ano;
    private Long totalCasos;
    private Map<Integer, Long> casosPorSemana;
    private String textoIa;
}
