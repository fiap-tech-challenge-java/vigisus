package br.com.fiap.vigisus.infrastructure.persistence;

import br.com.fiap.vigisus.application.port.RedeAssistencialPort;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedeAssistencialJpaAdapter implements RedeAssistencialPort {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final LeitoRepository leitoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;

    @Override
    public List<Leito> buscarLeitosPorTipoComMinimoSus(String tpLeito, int minQtSus) {
        return leitoRepository.findByTpLeitoAndQtSusGreaterThanEqual(tpLeito, minQtSus);
    }

    @Override
    public List<Estabelecimento> buscarEstabelecimentosPorCnes(Collection<String> coCnesList) {
        return estabelecimentoRepository.findByCoCnesIn(coCnesList);
    }

    @Override
    public List<ServicoEspecializado> buscarServicosPorCnes(Collection<String> coCnesList) {
        return servicoEspecializadoRepository.findByCoCnesIn(coCnesList);
    }

    @Override
    public List<Estabelecimento> buscarEstabelecimentosPorMunicipio(String coMunicipio) {
        return estabelecimentoRepository.findByMunicipio(coMunicipio);
    }

    @Override
    public List<Estabelecimento> buscarEstabelecimentosPorEstado(String uf) {
        return estabelecimentoRepository.findByEstado(uf);
    }
}
