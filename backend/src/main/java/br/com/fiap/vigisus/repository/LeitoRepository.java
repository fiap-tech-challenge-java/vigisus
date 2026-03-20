package br.com.fiap.vigisus.repository;

import br.com.fiap.vigisus.model.Leito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LeitoRepository extends JpaRepository<Leito, Long> {

    List<Leito> findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(Collection<String> coCnesList,
                                                                  String tpLeito,
                                                                  int minQtSus);

    List<Leito> findByTpLeitoAndQtSusGreaterThanEqual(String tpLeito, int minQtSus);
}
