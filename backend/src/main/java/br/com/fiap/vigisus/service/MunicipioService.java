package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MunicipioService {

    private final MunicipioRepository municipioRepository;

    public Municipio buscarPorCoIbge(String coIbge) {
        return municipioRepository.findByCoIbge(coIbge)
                .orElseThrow(() -> new MunicipioNotFoundException(coIbge));
    }

    public List<Municipio> listarPorUf(String uf) {
        return municipioRepository.findBySgUf(uf);
    }

    public List<Municipio> buscarPorNomeEUf(String nome, String uf) {
        return municipioRepository.findByNoMunicipioContainingIgnoreCaseAndSgUf(nome, uf);
    }

    public java.util.Optional<Municipio> buscarPorNome(String nome) {
        return municipioRepository.findTop1ByNoMunicipioContainingIgnoreCase(nome);
    }
}
