package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.CasoDengueRepository;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaminhamentoService {

    private static final List<String> SERVICOS_INFECCIOSAS = List.of("0135", "0136");

    /** Estimated fraction of dengue cases requiring hospital admission. */
    private static final double TAXA_INTERNACAO_ESTIMADA = 0.05;

    private final MunicipioService municipioService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final LeitoRepository leitoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;
    private final CasoDengueRepository casoDengueRepository;

    public EncaminhamentoResponse buscarHospitais(String coIbge, String tpLeito, int minLeitosSus) {
        Municipio origem = municipioService.buscarPorCoIbge(coIbge);

        // Step 1: CNES codes with infectious disease services
        Set<String> cnesComServico = servicoEspecializadoRepository
                .findDistinctCoCnesByServEspIn(SERVICOS_INFECCIOSAS);

        if (cnesComServico.isEmpty()) {
            return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, List.of(), "NORMAL");
        }

        // Step 2: Leitos matching bed type and minimum SUS quantity
        List<Leito> leitos = leitoRepository
                .findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(cnesComServico, tpLeito, minLeitosSus);

        if (leitos.isEmpty()) {
            return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, List.of(), "NORMAL");
        }

        // Map coCnes → qtSus (pick max in case of duplicates)
        Map<String, Integer> cnesParaQtSus = leitos.stream()
                .collect(Collectors.toMap(
                        Leito::getCoCnes,
                        Leito::getQtSus,
                        Integer::max));

        // Step 3: Find establishments for those CNES codes
        List<Estabelecimento> estabelecimentos = estabelecimentoRepository
                .findByCoCnesIn(cnesParaQtSus.keySet());

        double latOrigem = origem.getNuLatitude();
        double lonOrigem = origem.getNuLongitude();

        // Step 4: Build response with distances, sorted ascending
        List<HospitalDTO> hospitais = estabelecimentos.stream()
                .filter(e -> e.getNuLatitude() != null && e.getNuLongitude() != null)
                .map(e -> HospitalDTO.builder()
                        .coCnes(e.getCoCnes())
                        .noFantasia(e.getNoFantasia())
                        .coMunicipio(e.getCoMunicipio())
                        .nuTelefone(e.getNuTelefone())
                        .qtLeitosSus(cnesParaQtSus.getOrDefault(e.getCoCnes(), 0))
                        .distanciaKm(haversine(latOrigem, lonOrigem,
                                e.getNuLatitude(), e.getNuLongitude()))
                        .build())
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistanciaKm))
                .collect(Collectors.toList());

        int totalLeitosSus = cnesParaQtSus.values().stream().mapToInt(Integer::intValue).sum();
        String pressaoSus = calcularPressaoSus(coIbge, totalLeitosSus);

        return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, hospitais, pressaoSus);
    }

    private EncaminhamentoResponse buildResponse(String coIbge, String municipioOrigem,
                                                  String tpLeito, List<HospitalDTO> hospitais,
                                                  String pressaoSus) {
        return EncaminhamentoResponse.builder()
                .coIbge(coIbge)
                .municipioOrigem(municipioOrigem)
                .tpLeito(tpLeito)
                .hospitais(hospitais)
                .pressaoSus(pressaoSus)
                .build();
    }

    /**
     * Maps a clinical severity string to a CNES bed-type code (tp_leito).
     * <ul>
     *   <li>"grave" or "critica" → "81" (UTI adulto)</li>
     *   <li>anything else       → "74" (clínico adulto)</li>
     * </ul>
     *
     * @param gravidade severity label (case-insensitive)
     * @return tp_leito code
     */
    public String resolverTpLeito(String gravidade) {
        if (gravidade == null) {
            return "74";
        }
        String g = gravidade.strip().toLowerCase();
        if (g.equals("grave") || g.equals("critica") || g.equals("crítica")) {
            return "81";
        }
        return "74";
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     *
     * @return distance in kilometres
     */
    double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dlat = Math.toRadians(lat2 - lat1);
        double dlon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dlon / 2), 2);
        return R * 2 * Math.asin(Math.sqrt(a));
    }

    private String calcularPressaoSus(String coMunicipio, int leitosSus) {
        int anoAtual = Year.now().getValue();
        List<CasoDengue> recentes = casoDengueRepository
                .findByCoMunicipioAndAno(coMunicipio, anoAtual);

        if (recentes.isEmpty() || leitosSus == 0) return "NORMAL";

        long totalRecente = recentes.stream()
                .sorted(Comparator.comparingInt(CasoDengue::getSemanaEpi).reversed())
                .limit(4)
                .mapToLong(CasoDengue::getTotalCasos)
                .sum();

        double internacaoEstimada = totalRecente * TAXA_INTERNACAO_ESTIMADA;
        double ocupacaoEstimada = internacaoEstimada / leitosSus;

        if (ocupacaoEstimada < 0.5) return "NORMAL";
        if (ocupacaoEstimada < 0.75) return "ELEVADA";
        if (ocupacaoEstimada < 0.9) return "ALTA";
        return "CRITICA";
    }
}
