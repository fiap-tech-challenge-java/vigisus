package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaminhamentoService {

    // CNES service codes for infectious diseases / infectology
    private static final List<String> SERVICOS_INFECCIOSAS = List.of("135", "136");

    // Maximum number of hospitals returned per search
    private static final int MAX_HOSPITAIS = 5;

    private final MunicipioService municipioService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final LeitoRepository leitoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;

    public EncaminhamentoResponse buscarHospitais(String coIbge, String tpLeito, int minLeitosSus) {
        Municipio origem = municipioService.buscarPorCoIbge(coIbge);

        double lat = origem.getNuLatitude();
        double lon = origem.getNuLongitude();

        // Try increasing radii until at least one result is found
        int[] raiosKm = {50, 150, 300, 500};
        List<HospitalDTO> resultados = new ArrayList<>();

        for (int raio : raiosKm) {
            resultados = buscarNoRaio(lat, lon, tpLeito, minLeitosSus, raio);
            if (!resultados.isEmpty()) break;
        }

        return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, resultados);
    }

    private List<HospitalDTO> buscarNoRaio(double lat, double lon,
                                            String tpLeito, int minLeitosSus, int raioKm) {
        List<Leito> leitos = leitoRepository
                .findByTpLeitoAndQtSusGreaterThanEqual(tpLeito, minLeitosSus);

        if (leitos.isEmpty()) {
            return List.of();
        }

        // Pre-fetch establishments and specialized services to avoid N+1 queries
        Set<String> cnesSet = leitos.stream().map(Leito::getCoCnes).collect(Collectors.toSet());

        Map<String, Estabelecimento> estPorCnes = estabelecimentoRepository
                .findByCoCnesIn(cnesSet).stream()
                .collect(Collectors.toMap(Estabelecimento::getCoCnes, e -> e, (a, b) -> a));

        Map<String, List<ServicoEspecializado>> servicosPorCnes =
                servicoEspecializadoRepository.findByCoCnesIn(cnesSet).stream()
                        .collect(Collectors.groupingBy(ServicoEspecializado::getCoCnes));

        List<HospitalDTO> resultado = new ArrayList<>();

        for (Leito leito : leitos) {
            Estabelecimento est = estPorCnes.get(leito.getCoCnes());

            if (est == null
                    || est.getNuLatitude() == null
                    || est.getNuLongitude() == null) continue;

            double dist = haversine(lat, lon, est.getNuLatitude(), est.getNuLongitude());

            if (dist > raioKm) continue;

            boolean temInfecciosas = servicosPorCnes
                    .getOrDefault(leito.getCoCnes(), List.of())
                    .stream()
                    .anyMatch(s -> SERVICOS_INFECCIOSAS.contains(s.getServEsp()));

            resultado.add(HospitalDTO.builder()
                    .coCnes(est.getCoCnes())
                    .noFantasia(est.getNoFantasia())
                    .coMunicipio(est.getCoMunicipio())
                    .nuTelefone(est.getNuTelefone())
                    .qtLeitosSus(leito.getQtSus())
                    .distanciaKm(Math.round(dist * 10.0) / 10.0)
                    .servicoInfectologia(temInfecciosas)
                    .build());
        }

        resultado.sort(Comparator.comparingDouble(HospitalDTO::getDistanciaKm));
        return resultado.stream().limit(MAX_HOSPITAIS).collect(Collectors.toList());
    }

    private EncaminhamentoResponse buildResponse(String coIbge, String municipioOrigem,
                                                  String tpLeito, List<HospitalDTO> hospitais) {
        return EncaminhamentoResponse.builder()
                .coIbge(coIbge)
                .municipioOrigem(municipioOrigem)
                .tpLeito(tpLeito)
                .hospitais(hospitais)
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
}
