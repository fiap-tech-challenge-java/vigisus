package br.com.fiap.vigisus.domain.risco;

import java.time.LocalDate;

/**
 * Evento de domínio: risco epidemiológico alto detectado para um município.
 * Publicado pelo use case de previsão de risco quando o score ultrapassa
 * o limiar de classificação ALTO ou MUITO_ALTO.
 */
public record RiscoAltoDetectadoEvent(
    String coIbge,
    String nomeMunicipio,
    String uf,
    String classificacao,        // "ALTO" ou "MUITO_ALTO"
    double scoreRisco,
    double temperaturaMedia,
    double chuvaAcumulada,
    LocalDate dataDeteccao
) {
    public boolean isMuitoAlto() {
        return "MUITO_ALTO".equals(classificacao);
    }
}
