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
public class EncaminhamentoResponse {

    private String coIbge;
    private String municipioOrigem;
    private String tpLeito;
    private List<HospitalDTO> hospitais;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalDTO {

        private String coCnes;
        private String noFantasia;
        private String coMunicipio;
        private String nuTelefone;
        private int qtLeitosSus;
        private double distanciaKm;
        private boolean servicoInfectologia;
    }
}
