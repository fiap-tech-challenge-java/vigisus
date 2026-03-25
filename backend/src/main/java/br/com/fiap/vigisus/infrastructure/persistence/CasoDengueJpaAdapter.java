package br.com.fiap.vigisus.infrastructure.persistence;

import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CasoDengueJpaAdapter implements CasoDenguePort {

    private final CasoDengueRepository casoDengueRepository;

    @Override
    public Long sumTotalCasosByCoMunicipioAndAno(String coMunicipio, int ano) {
        return casoDengueRepository.sumTotalCasosByCoMunicipioAndAno(coMunicipio, ano);
    }

    @Override
    public List<Object[]> findCasosPorSemanas(String coMunicipio, int ano, List<Integer> semanas) {
        return casoDengueRepository.findCasosPorSemanas(coMunicipio, ano, semanas);
    }

    @Override
    public List<CasoDengue> findByCoMunicipioAndAnoOrderBySemanaEpiAsc(String coMunicipio, int ano) {
        return casoDengueRepository.findByCoMunicipioAndAnoOrderBySemanaEpiAsc(coMunicipio, ano);
    }

    @Override
    public List<CasoDengue> findByCoMunicipioAndAno(String coMunicipio, int ano) {
        return casoDengueRepository.findByCoMunicipioAndAno(coMunicipio, ano);
    }

    @Override
    public List<Object[]> agregaCasosPorMunicipioNoAno(int ano) {
        return casoDengueRepository.agregaCasosPorMunicipioNoAno(ano);
    }

    @Override
    public List<Object[]> agregaCasosPorEstadoNoAno(int ano) {
        return casoDengueRepository.agregaCasosPorEstadoNoAno(ano);
    }

    @Override
    public List<Object[]> agregaSemanasBrasil(int ano) {
        return casoDengueRepository.agregaSemanasBrasil(ano);
    }

    @Override
    public List<Object[]> rankingOtimizadoPorEstado(String uf, int ano) {
        return casoDengueRepository.rankingOtimizadoPorEstado(uf, ano);
    }

    @Override
    public List<Object[]> agregaSemanasPorEstado(String uf, int ano) {
        return casoDengueRepository.agregaSemanasPorEstado(uf, ano);
    }

    @Override
    public List<Object[]> agregaTotaisEstadoNoAno(String uf, int ano) {
        return casoDengueRepository.agregaTotaisEstadoNoAno(uf, ano);
    }
}
