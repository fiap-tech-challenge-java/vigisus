package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    List<Estabelecimento> findByCoCnesIn(Collection<String> coCnesList);

    Optional<Estabelecimento> findByCoCnes(String coCnes);

    /**
     * Encontra estabelecimentos de um estado via join com Municipio
     */
    @Query("SELECT e FROM Estabelecimento e, Municipio m " +
           "WHERE e.coMunicipio = m.coIbge " +
           "AND m.sgUf = :uf " +
           "ORDER BY e.noFantasia ASC")
    List<Estabelecimento> findByEstado(@Param("uf") String uf);

    /**
     * Encontra estabelecimentos de um município específico
     */
    @Query("SELECT e FROM Estabelecimento e " +
           "WHERE e.coMunicipio = :coMunicipio " +
           "ORDER BY e.noFantasia ASC")
    List<Estabelecimento> findByMunicipio(@Param("coMunicipio") String coMunicipio);
}
