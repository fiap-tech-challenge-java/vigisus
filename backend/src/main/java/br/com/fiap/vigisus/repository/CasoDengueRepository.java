package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.CasoDengue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CasoDengueRepository extends JpaRepository<CasoDengue, Long> {

    @Query("SELECT COALESCE(SUM(c.totalCasos), 0) FROM CasoDengue c " +
            "WHERE c.coMunicipio = :coMunicipio AND c.ano = :ano")
    Long sumTotalCasosByCoMunicipioAndAno(@Param("coMunicipio") String coMunicipio,
                                          @Param("ano") int ano);
}
