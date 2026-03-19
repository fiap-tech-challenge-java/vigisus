package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.CasoDengue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CasoDengueRepository extends JpaRepository<CasoDengue, Long> {

    @Query("SELECT COALESCE(SUM(c.totalCasos), 0) FROM CasoDengue c " +
            "WHERE c.coMunicipio = :coMunicipio AND c.ano = :ano")
    Long sumTotalCasosByCoMunicipioAndAno(@Param("coMunicipio") String coMunicipio,
                                          @Param("ano") int ano);

    @Query("SELECT c.coMunicipio, COALESCE(SUM(c.totalCasos), 0) FROM CasoDengue c " +
            "WHERE c.coMunicipio IN :cosMunicipios AND c.ano = :ano " +
            "GROUP BY c.coMunicipio")
    List<Object[]> sumTotalCasosByCoMunicipioInAndAno(@Param("cosMunicipios") Collection<String> cosMunicipios,
                                                       @Param("ano") int ano);

    @Query("SELECT c.semanaEpi, COALESCE(SUM(c.totalCasos), 0) FROM CasoDengue c " +
            "WHERE c.coMunicipio = :coMunicipio AND c.ano = :ano AND c.semanaEpi IN :semanas " +
            "GROUP BY c.semanaEpi ORDER BY c.semanaEpi")
    List<Object[]> findCasosPorSemanas(@Param("coMunicipio") String coMunicipio,
                                       @Param("ano") int ano,
                                       @Param("semanas") List<Integer> semanas);
}
