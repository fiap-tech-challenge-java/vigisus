package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.ServicoEspecializado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface ServicoEspecializadoRepository extends JpaRepository<ServicoEspecializado, Long> {

    @Query("SELECT DISTINCT s.coCnes FROM ServicoEspecializado s WHERE s.servEsp IN :servEsps")
    Set<String> findDistinctCoCnesByServEspIn(@Param("servEsps") Collection<String> servEsps);

    List<ServicoEspecializado> findByCoCnes(String coCnes);
}
