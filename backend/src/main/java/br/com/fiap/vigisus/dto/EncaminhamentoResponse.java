package br.com.fiap.vigisus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Resposta com hospitais de referência para encaminhamento do paciente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncaminhamentoResponse {

    @Schema(description = "Código IBGE do município de origem", example = "3509502")
    private String coIbge;

    @Schema(description = "Nome do município de origem", example = "Campinas")
    private String municipioOrigem;

    @Schema(description = "Tipo de leito necessário", example = "CLINICO")
    private String tpLeito;

    @Schema(description = "Lista de hospitais disponíveis para encaminhamento")
    private List<HospitalDTO> hospitais;

    @Schema(description = "Nível de pressão do SUS no município", example = "ELEVADO")
    // Health system pressure calculated from estimated bed occupancy
    private String pressaoSus;

    @Schema(description = "Dados de um hospital de referência")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalDTO {

        @Schema(description = "Código CNES do estabelecimento", example = "2077485")
        private String coCnes;

        @Schema(description = "Nome fantasia do hospital", example = "Hospital das Clínicas de Campinas")
        private String noFantasia;

        @Schema(description = "Código do município onde o hospital está localizado", example = "3509502")
        private String coMunicipio;

        @Schema(description = "Telefone de contato do hospital", example = "(19) 3521-7000")
        private String nuTelefone;

        @Schema(description = "Quantidade de leitos SUS disponíveis", example = "120")
        private int qtLeitosSus;

        @Schema(description = "Distância em km do município de origem", example = "5.3")
        private double distanciaKm;

        @Schema(description = "Indica se o hospital possui serviço de infectologia", example = "true")
        private boolean servicoInfectologia;

        @Schema(description = "Latitude geográfica do hospital", example = "-22.9140")
        // Geographic coordinates for map markers
        private Double nuLatitude;

        @Schema(description = "Longitude geográfica do hospital", example = "-47.0628")
        private Double nuLongitude;
    }
}
