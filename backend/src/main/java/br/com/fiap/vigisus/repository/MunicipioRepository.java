package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Optional<Municipio> findByCoIbge(String coIbge);

    Optional<Municipio> findByNoMunicipioIgnoreCaseAndSgUfIgnoreCase(String noMunicipio, String sgUf);
}
