package br.com.fiap.vigisus.application.port;

import br.com.fiap.vigisus.model.CasoDengue;

import java.util.List;

public interface CasoDenguePort {

    Long sumTotalCasosByCoMunicipioAndAno(String coMunicipio, int ano);

    List<Object[]> findCasosPorSemanas(String coMunicipio, int ano, List<Integer> semanas);

    List<CasoDengue> findByCoMunicipioAndAnoOrderBySemanaEpiAsc(String coMunicipio, int ano);

    List<CasoDengue> findByCoMunicipioAndAno(String coMunicipio, int ano);

    List<Object[]> agregaCasosPorMunicipioNoAno(int ano);

    List<Object[]> agregaCasosPorEstadoNoAno(int ano);

    List<Object[]> agregaSemanasBrasil(int ano);

    List<Object[]> rankingOtimizadoPorEstado(String uf, int ano);

    List<Object[]> agregaSemanasPorEstado(String uf, int ano);

    List<Object[]> agregaTotaisEstadoNoAno(String uf, int ano);
}
