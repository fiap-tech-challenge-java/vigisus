package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.CasoDengue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasoDengueRepository extends JpaRepository<CasoDengue, Long> {

    List<CasoDengue> findByCoMunicipioAndAno(String coMunicipio, Integer ano);
}
