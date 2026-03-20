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

    /** NORMAL | ELEVADO | CRITICO */
    private String nivelAtencao;

    /** Texto descritivo da situação — sem verbo imperativo */
    private String contextoAtual;

    /** Comparação com mesmo período de anos anteriores */
    private String padraoHistorico;

    /** Lista informativa — linguagem descritiva, não imperativa */
    private List<String> checklistInformativo;

    private ContextoEpidemiologicoDTO contexto;
    private PrevisaoProximosDiasDTO previsao;
    private List<HospitalDTO> hospitaisReferencia;
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
}
