package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;

public interface PrevisaoRiscoService {

    PrevisaoRiscoResponse calcularRisco(String coIbge, String doenca);
}
