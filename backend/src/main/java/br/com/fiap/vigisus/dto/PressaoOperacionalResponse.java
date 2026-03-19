package br.com.fiap.vigisus.dto;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PressaoOperacionalResponse {

    private String municipio;
    private String tipoUnidade;
    private String nivelPressao;

    private ContextoEpidemiologicoDTO contexto;
    private PrevisaoProximosDiasDTO previsao;
    private List<HospitalDTO> hospitaisReferencia;
    private RecomendacaoOperacionalDTO recomendacao;
    private String textoIa;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextoEpidemiologicoDTO {
        private String classificacaoAtual;
        private int casosUltimasSemanas;
        private String tendencia;
        private String comparativoHistorico;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrevisaoProximosDiasDTO {
        private String riscoClimatico;
        private String tendencia7Dias;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecomendacaoOperacionalDTO {
        private String acao;
        private String justificativa;
        private List<String> medidasSugeridas;
    }
}
