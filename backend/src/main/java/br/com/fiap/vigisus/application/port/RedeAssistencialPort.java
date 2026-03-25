package br.com.fiap.vigisus.application.port;

import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.ServicoEspecializado;

import java.util.Collection;
import java.util.List;

public interface RedeAssistencialPort {

    List<Leito> buscarLeitosPorTipoComMinimoSus(String tpLeito, int minQtSus);

    List<Estabelecimento> buscarEstabelecimentosPorCnes(Collection<String> coCnesList);

    List<ServicoEspecializado> buscarServicosPorCnes(Collection<String> coCnesList);

    List<Estabelecimento> buscarEstabelecimentosPorMunicipio(String coMunicipio);

    List<Estabelecimento> buscarEstabelecimentosPorEstado(String uf);
}
