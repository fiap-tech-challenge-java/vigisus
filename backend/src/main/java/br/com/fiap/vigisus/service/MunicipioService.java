package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.MunicipioPort;
import br.com.fiap.vigisus.exception.MunicipioNotFoundException;
import br.com.fiap.vigisus.model.Municipio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Camada de orquestração que coordena use cases e integrações externas.
 *
 * <p>Depende exclusivamente de Ports (application/port/) — nunca de repositórios
 * JPA diretamente — respeitando a regra de dependência da Clean Architecture.
 *
 * <p>Candidato à migração para use case dedicado na versão 2.0.
 */
@Service
@RequiredArgsConstructor
public class MunicipioService {

    private final MunicipioPort municipioPort;

    public Municipio buscarPorCoIbge(String coIbge) {
        return municipioPort.findByCoIbge(coIbge)
                .orElseThrow(() -> new MunicipioNotFoundException(coIbge));
    }

    public List<Municipio> listarPorUf(String uf) {
        return municipioPort.findBySgUf(uf);
    }

    public List<Municipio> buscarPorNomeEUf(String nome, String uf) {
        return municipioPort.findByNoMunicipioContainingIgnoreCaseAndSgUf(nome, uf);
    }

    public java.util.Optional<Municipio> buscarPorNome(String nome) {
        return municipioPort.findTop1ByNoMunicipioContainingIgnoreCase(nome);
    }
}
