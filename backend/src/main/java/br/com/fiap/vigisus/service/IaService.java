package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.IntencaoDTO;
import br.com.fiap.vigisus.dto.PerfilEpidemiologicoResponse;
import br.com.fiap.vigisus.dto.PrevisaoRiscoResponse;

import java.util.List;

public interface IaService {

    String gerarTextoEpidemiologico(PerfilEpidemiologicoResponse perfil);

    String gerarTextoRisco(PrevisaoRiscoResponse previsao);

    IntencaoDTO interpretarPergunta(String pergunta);

    String gerarTextoTriagem(String prioridade, List<String> sintomas, String alertaEpidemiologico);

    String gerarTextoOperacional(String contexto);

    String gerarTextoBuscaCompleta(String contexto);
}
