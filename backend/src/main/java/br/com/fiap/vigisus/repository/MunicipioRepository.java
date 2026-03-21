package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Optional<Municipio> findByCoIbge(String coIbge);

    List<Municipio> findBySgUf(String sgUf);

    List<Municipio> findByNoMunicipioContainingIgnoreCaseAndSgUf(String nome, String sgUf);

    Optional<Municipio> findTop1ByNoMunicipioContainingIgnoreCase(String nome);
}
