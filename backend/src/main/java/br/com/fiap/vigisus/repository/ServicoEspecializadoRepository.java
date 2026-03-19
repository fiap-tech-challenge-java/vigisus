package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.ServicoEspecializado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoEspecializadoRepository extends JpaRepository<ServicoEspecializado, Long> {

    List<ServicoEspecializado> findByCoCnesAndServEsp(String coCnes, String servEsp);

    @Query("SELECT DISTINCT s.coCnes FROM ServicoEspecializado s WHERE s.servEsp = :servEsp")
    List<String> findCoCnesByServEsp(@Param("servEsp") String servEsp);
}
