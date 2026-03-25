package br.com.fiap.vigisus.application.port;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;

import java.util.List;

public interface ClimaPort {

    ClimaAtualDTO buscarClimaAtual(double lat, double lon);

    List<PrevisaoDiariaDTO> buscarPrevisao16Dias(double lat, double lon);
}
