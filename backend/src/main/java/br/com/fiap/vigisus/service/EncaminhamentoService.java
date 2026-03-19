package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.HospitalDTO;

import java.util.List;

public interface EncaminhamentoService {

    EncaminhamentoResponse buscarHospitais(String coIbge, String condicao, String gravidade);

    List<HospitalDTO> buscarHospitaisPorRaio(String coIbge, String servico, Integer raioKm, String tpLeito);
}
