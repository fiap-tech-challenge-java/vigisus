package br.com.fiap.vigisus.application.port;

import br.com.fiap.vigisus.model.Municipio;

import java.util.List;
import java.util.Optional;

public interface MunicipioPort {

    Optional<Municipio> findByCoIbge(String coIbge);

    List<Municipio> findBySgUf(String uf);

    List<Municipio> findAll();

    List<Municipio> findByNoMunicipioContainingIgnoreCaseAndSgUf(String nome, String uf);

    Optional<Municipio> findTop1ByNoMunicipioContainingIgnoreCase(String nome);
}
