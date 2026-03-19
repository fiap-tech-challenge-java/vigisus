package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    List<Estabelecimento> findByCoMunicipio(String coMunicipio);

    List<Estabelecimento> findByCoCnesIn(Set<String> coCnes);
}
