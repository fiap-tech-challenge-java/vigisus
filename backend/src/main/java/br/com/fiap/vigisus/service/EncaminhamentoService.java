package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.repository.EstabelecimentoRepository;
import br.com.fiap.vigisus.repository.LeitoRepository;
import br.com.fiap.vigisus.repository.ServicoEspecializadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaminhamentoService {

    // CNES service codes for infectious diseases / infectology
    private static final List<String> SERVICOS_INFECCIOSAS = List.of("0135", "0136");

    private final MunicipioService municipioService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final LeitoRepository leitoRepository;
    private final ServicoEspecializadoRepository servicoEspecializadoRepository;

    public EncaminhamentoResponse buscarHospitais(String coIbge, String tpLeito, int minLeitosSus) {
        Municipio origem = municipioService.buscarPorCoIbge(coIbge);

        // Step 1: CNES codes with infectious disease services
        Set<String> cnesComServico = servicoEspecializadoRepository
                .findDistinctCoCnesByServEspIn(SERVICOS_INFECCIOSAS);

        if (cnesComServico.isEmpty()) {
            return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, List.of());
        }

        // Step 2: Leitos matching bed type and minimum SUS quantity
        List<Leito> leitos = leitoRepository
                .findByCoCnesInAndTpLeitoAndQtSusGreaterThanEqual(cnesComServico, tpLeito, minLeitosSus);

        if (leitos.isEmpty()) {
            return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, List.of());
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

        return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, hospitais);
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
