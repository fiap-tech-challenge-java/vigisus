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

    List<CasoDengue> findByCoMunicipioAndAnoOrderBySemanaEpiAsc(String coMunicipio, int ano);

    List<CasoDengue> findByCoMunicipioAndAno(String coMunicipio, int ano);

    // ============================================================
    // QUERIES OTIMIZADAS PARA PERFORMANCE COM 50K+ REGISTROS
    // ============================================================

    /**
     * Agregar TODOS os casos por município em UMA ÚNICA QUERY
     * Resultado: (co_municipio, total_casos)
     */
    @Query(value = "SELECT c.co_municipio, COALESCE(SUM(c.total_casos), 0) as total " +
            "FROM casos_dengue c " +
            "WHERE c.ano = :ano " +
            "GROUP BY c.co_municipio " +
            "ORDER BY total DESC",
            nativeQuery = true)
    List<Object[]> agregaCasosPorMunicipioNoAno(@Param("ano") int ano);

    /**
     * Agregar por estado com JOIN de população
     * Resultado: (sg_uf, total_casos, populacao)
     */
    @Query(value = "SELECT m.sg_uf, COALESCE(SUM(c.total_casos), 0) as total, " +
            "       COALESCE(SUM(m.populacao), 0) as populacao " +
            "FROM municipios m " +
            "LEFT JOIN ( " +
            "    SELECT cd.co_municipio, SUM(cd.total_casos) as total_casos " +
            "    FROM casos_dengue cd " +
            "    WHERE cd.ano = :ano " +
            "    GROUP BY cd.co_municipio " +
            ") c ON c.co_municipio = m.co_ibge " +
            "GROUP BY m.sg_uf " +
            "ORDER BY total DESC",
            nativeQuery = true)
    List<Object[]> agregaCasosPorEstadoNoAno(@Param("ano") int ano);

    /**
     * Agregar semanas epidemiológicas por estado (Brasil inteiro)
     */
    @Query(value = "SELECT c.semana_epi, COALESCE(SUM(c.total_casos), 0) as total " +
            "FROM casos_dengue c " +
            "WHERE c.ano = :ano " +
            "GROUP BY c.semana_epi " +
            "ORDER BY c.semana_epi ASC",
            nativeQuery = true)
    List<Object[]> agregaSemanasBrasil(@Param("ano") int ano);

    /**
     * Ranking otimizado: uma única query com todas as infos
     * Resultado: (co_ibge, municipio, sg_uf, total_casos, populacao)
     */
    @Query(value = "SELECT m.co_ibge, m.no_municipio, m.sg_uf, " +
            "       COALESCE(SUM(c.total_casos), 0) as total_casos, " +
            "       m.populacao " +
            "FROM municipios m " +
            "LEFT JOIN casos_dengue c ON m.co_ibge = c.co_municipio AND c.ano = :ano " +
            "WHERE m.sg_uf = :uf " +
            "GROUP BY m.co_ibge, m.no_municipio, m.sg_uf, m.populacao " +
            "HAVING m.populacao > 0 " +
            "ORDER BY total_casos DESC ",
            nativeQuery = true)
    List<Object[]> rankingOtimizadoPorEstado(@Param("uf") String uf, @Param("ano") int ano);

    /**
     * Agrega semanas epidemiológicas de um estado no ano.
     * Resultado: (semana_epi, total_casos)
     */
    @Query(value = "SELECT c.semana_epi, COALESCE(SUM(c.total_casos), 0) as total " +
            "FROM casos_dengue c " +
            "INNER JOIN municipios m ON m.co_ibge = c.co_municipio " +
            "WHERE m.sg_uf = :uf AND c.ano = :ano " +
            "GROUP BY c.semana_epi " +
            "ORDER BY c.semana_epi ASC",
            nativeQuery = true)
    List<Object[]> agregaSemanasPorEstado(@Param("uf") String uf, @Param("ano") int ano);

    /**
     * Total de casos e população de um estado no ano sem duplicar população por semana.
     * Resultado: (total_casos, populacao)
     */
    @Query(value = "SELECT COALESCE(SUM(c.total_casos), 0) as total, COALESCE(SUM(m.populacao), 0) as populacao " +
            "FROM municipios m " +
            "LEFT JOIN ( " +
            "    SELECT cd.co_municipio, SUM(cd.total_casos) as total_casos " +
            "    FROM casos_dengue cd " +
            "    WHERE cd.ano = :ano " +
            "    GROUP BY cd.co_municipio " +
            ") c ON c.co_municipio = m.co_ibge " +
            "WHERE m.sg_uf = :uf",
            nativeQuery = true)
        List<Object[]> agregaTotaisEstadoNoAno(@Param("uf") String uf, @Param("ano") int ano);
}
