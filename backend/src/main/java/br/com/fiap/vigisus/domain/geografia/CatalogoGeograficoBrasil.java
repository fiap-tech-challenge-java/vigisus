package br.com.fiap.vigisus.domain.geografia;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class CatalogoGeograficoBrasil {

    private static final double[] CENTROIDE_PADRAO = new double[]{-15.7801, -47.9292};
    private static final Map<String, String> NOMES_ESTADOS = criarNomesEstados();
    private static final Map<String, double[]> CENTROIDES_ESTADOS = criarCentroidesEstados();
    private static final Map<String, String> CAPITAIS_RISCO_AGREGADO = criarCapitaisRiscoAgregado();
    private static final Map<String, String> CAPITAIS_ENCAMINHAMENTO = criarCapitaisEncaminhamento();

    public String nomeEstado(String uf) {
        return NOMES_ESTADOS.getOrDefault(normalizarUf(uf), uf);
    }

    public double[] centroideEstado(String uf) {
        double[] centroide = CENTROIDES_ESTADOS.get(normalizarUf(uf));
        return centroide != null ? centroide.clone() : CENTROIDE_PADRAO.clone();
    }

    public String codigoCapitalRiscoAgregado(String uf) {
        return CAPITAIS_RISCO_AGREGADO.get(normalizarUf(uf));
    }

    public String codigoCapitalEncaminhamento(String uf) {
        return CAPITAIS_ENCAMINHAMENTO.get(normalizarUf(uf));
    }

    private String normalizarUf(String uf) {
        return uf == null ? null : uf.trim().toUpperCase();
    }

    private static Map<String, String> criarNomesEstados() {
        Map<String, String> estados = new HashMap<>();
        estados.put("AC", "Acre");
        estados.put("AL", "Alagoas");
        estados.put("AP", "Amapá");
        estados.put("AM", "Amazonas");
        estados.put("BA", "Bahia");
        estados.put("CE", "Ceará");
        estados.put("DF", "Distrito Federal");
        estados.put("ES", "Espírito Santo");
        estados.put("GO", "Goiás");
        estados.put("MA", "Maranhão");
        estados.put("MT", "Mato Grosso");
        estados.put("MS", "Mato Grosso do Sul");
        estados.put("MG", "Minas Gerais");
        estados.put("PA", "Pará");
        estados.put("PB", "Paraíba");
        estados.put("PR", "Paraná");
        estados.put("PE", "Pernambuco");
        estados.put("PI", "Piauí");
        estados.put("RJ", "Rio de Janeiro");
        estados.put("RN", "Rio Grande do Norte");
        estados.put("RS", "Rio Grande do Sul");
        estados.put("RO", "Rondônia");
        estados.put("RR", "Roraima");
        estados.put("SC", "Santa Catarina");
        estados.put("SP", "São Paulo");
        estados.put("SE", "Sergipe");
        estados.put("TO", "Tocantins");
        return Collections.unmodifiableMap(estados);
    }

    private static Map<String, double[]> criarCentroidesEstados() {
        Map<String, double[]> centroides = new HashMap<>();
        centroides.put("MG", new double[]{-18.5122, -44.5550});
        centroides.put("SP", new double[]{-22.1875, -48.7966});
        centroides.put("RJ", new double[]{-22.2500, -42.6667});
        centroides.put("ES", new double[]{-19.1834, -40.3089});
        centroides.put("BA", new double[]{-12.5797, -41.7007});
        centroides.put("GO", new double[]{-15.9670, -49.8319});
        centroides.put("DF", new double[]{-15.7217, -47.9292});
        centroides.put("PR", new double[]{-24.8900, -51.5550});
        centroides.put("SC", new double[]{-27.2423, -50.2189});
        centroides.put("RS", new double[]{-30.0346, -51.2177});
        centroides.put("MT", new double[]{-12.6819, -56.9211});
        centroides.put("MS", new double[]{-20.7722, -54.7852});
        centroides.put("PA", new double[]{-3.4168, -52.0000});
        centroides.put("AM", new double[]{-3.4168, -65.0000});
        centroides.put("CE", new double[]{-5.4984, -39.3206});
        centroides.put("PE", new double[]{-8.8137, -36.9541});
        return Collections.unmodifiableMap(centroides);
    }

    private static Map<String, String> criarCapitaisRiscoAgregado() {
        Map<String, String> capitais = new HashMap<>();
        capitais.put("AC", "1100015");
        capitais.put("AL", "2700104");
        capitais.put("AP", "1600055");
        capitais.put("AM", "1302603");
        capitais.put("BA", "2902404");
        capitais.put("CE", "2304400");
        capitais.put("DF", "5300108");
        capitais.put("ES", "3200100");
        capitais.put("GO", "5208707");
        capitais.put("MA", "2111300");
        capitais.put("MT", "5103403");
        capitais.put("MS", "5002704");
        capitais.put("MG", "3106200");
        capitais.put("PA", "1505714");
        capitais.put("PB", "2507002");
        capitais.put("PR", "4106902");
        capitais.put("PE", "2611606");
        capitais.put("PI", "2211001");
        capitais.put("RJ", "3304557");
        capitais.put("RN", "2408102");
        capitais.put("RS", "4314902");
        capitais.put("RO", "1100122");
        capitais.put("RR", "1400100");
        capitais.put("SC", "4205402");
        capitais.put("SP", "3550308");
        capitais.put("SE", "2800308");
        capitais.put("TO", "2710202");
        return Collections.unmodifiableMap(capitais);
    }

    private static Map<String, String> criarCapitaisEncaminhamento() {
        Map<String, String> capitais = new HashMap<>();
        capitais.put("AC", "1200401");
        capitais.put("AL", "2704302");
        capitais.put("AP", "1600407");
        capitais.put("AM", "1302603");
        capitais.put("BA", "2904144");
        capitais.put("CE", "2304400");
        capitais.put("DF", "5300108");
        capitais.put("ES", "3200144");
        capitais.put("GO", "5208050");
        capitais.put("MA", "2111300");
        capitais.put("MT", "5103403");
        capitais.put("MS", "2813602");
        capitais.put("MG", "3106200");
        capitais.put("PA", "1505402");
        capitais.put("PB", "2507202");
        capitais.put("PR", "4106902");
        capitais.put("PE", "2611606");
        capitais.put("PI", "2211001");
        capitais.put("RJ", "3304557");
        capitais.put("RN", "2408102");
        capitais.put("RS", "4314902");
        capitais.put("RO", "1703260");
        capitais.put("RR", "1400100");
        capitais.put("SC", "4204202");
        capitais.put("SP", "3550308");
        capitais.put("SE", "2800308");
        capitais.put("TO", "2810004");
        return Collections.unmodifiableMap(capitais);
    }
}
