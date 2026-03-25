package br.com.fiap.vigisus.infrastructure.persistence;

import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MunicipioJpaAdapter implements MunicipioPort {

    private final MunicipioRepository municipioRepository;

    @Override
    public Optional<Municipio> findByCoIbge(String coIbge) {
        return municipioRepository.findByCoIbge(coIbge);
    }

    @Override
    public List<Municipio> findBySgUf(String uf) {
        return municipioRepository.findBySgUf(uf);
    }

    @Override
    public List<Municipio> findAll() {
        return municipioRepository.findAll();
    }

    @Override
    public List<Municipio> findByNoMunicipioContainingIgnoreCaseAndSgUf(String nome, String uf) {
        return municipioRepository.findByNoMunicipioContainingIgnoreCaseAndSgUf(nome, uf);
    }

    @Override
    public Optional<Municipio> findTop1ByNoMunicipioContainingIgnoreCase(String nome) {
        return municipioRepository.findTop1ByNoMunicipioContainingIgnoreCase(nome);
    }
}
