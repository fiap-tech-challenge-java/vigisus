package br.com.fiap.vigisus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDTO {

    private String coCnes;
    private String noFantasia;
    private String municipio;
    private String telefone;
    private Double distanciaKm;
    private Double latitude;
    private Double longitude;
}
