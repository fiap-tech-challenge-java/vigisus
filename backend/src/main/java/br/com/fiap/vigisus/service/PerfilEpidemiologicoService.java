package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;

public interface PerfilEpidemiologicoService {

    PerfilEpidemiologicoResponse gerarPerfil(String coIbge, String doenca, Integer ano);
}
