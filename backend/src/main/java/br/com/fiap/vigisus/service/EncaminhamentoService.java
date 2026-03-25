package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.encaminhamento.SelecionadorHospitaisProximos;
import br.com.fiap.vigisus.application.port.CasoDenguePort;
import br.com.fiap.vigisus.application.port.RedeAssistencialPort;
import br.com.fiap.vigisus.domain.encaminhamento.ClassificacaoPressaoSusPolicy;
import br.com.fiap.vigisus.domain.geografia.CalculadoraDistanciaGeografica;
import br.com.fiap.vigisus.domain.geografia.CatalogoGeograficoBrasil;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse;
import br.com.fiap.vigisus.dto.EncaminhamentoResponse.HospitalDTO;
import br.com.fiap.vigisus.model.CasoDengue;
import br.com.fiap.vigisus.model.Estabelecimento;
import br.com.fiap.vigisus.model.Leito;
import br.com.fiap.vigisus.model.Municipio;
import br.com.fiap.vigisus.model.ServicoEspecializado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EncaminhamentoService {

    private static final List<String> UFS_BRASIL = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private static final int MAX_HOSPITAIS = 5;

    private final MunicipioService municipioService;
    private final RedeAssistencialPort redeAssistencialPort;
    private final CasoDenguePort casoDenguePort;
    private final CatalogoGeograficoBrasil catalogoGeograficoBrasil;
    private final CalculadoraDistanciaGeografica calculadoraDistanciaGeografica;
    private final ClassificacaoPressaoSusPolicy classificacaoPressaoSusPolicy;
    private final SelecionadorHospitaisProximos selecionadorHospitaisProximos;

    public EncaminhamentoResponse buscarHospitais(String coIbge, String tpLeito, int minLeitosSus) {
        Municipio origem = municipioService.buscarPorCoIbge(coIbge);

        double lat = origem.getNuLatitude();
        double lon = origem.getNuLongitude();

        int[] raiosKm = {50, 150, 300, 500};
        List<HospitalDTO> resultados = new ArrayList<>();

        for (int raio : raiosKm) {
            resultados = buscarNoRaio(lat, lon, tpLeito, minLeitosSus, raio);
            if (!resultados.isEmpty()) {
                break;
            }
        }

        int totalLeitosSus = resultados.stream().mapToInt(HospitalDTO::getQtLeitosSus).sum();
        String pressaoSus = calcularPressaoSus(coIbge, totalLeitosSus);

        return buildResponse(coIbge, origem.getNoMunicipio(), tpLeito, resultados, pressaoSus);
    }

    private List<HospitalDTO> buscarNoRaio(double lat, double lon, String tpLeito, int minLeitosSus, int raioKm) {
        List<Leito> leitos = redeAssistencialPort.buscarLeitosPorTipoComMinimoSus(tpLeito, minLeitosSus);
        if (leitos.isEmpty()) {
            return List.of();
        }

        Set<String> cnesSet = leitos.stream().map(Leito::getCoCnes).collect(Collectors.toSet());

        Map<String, Estabelecimento> estPorCnes = redeAssistencialPort.buscarEstabelecimentosPorCnes(cnesSet).stream()
                .collect(Collectors.toMap(Estabelecimento::getCoCnes, estabelecimento -> estabelecimento, (a, b) -> a));

        Map<String, List<ServicoEspecializado>> servicosPorCnes = redeAssistencialPort.buscarServicosPorCnes(cnesSet)
                .stream()
                .collect(Collectors.groupingBy(ServicoEspecializado::getCoCnes));

        return selecionadorHospitaisProximos.selecionar(
                lat,
                lon,
                leitos,
                estPorCnes,
                servicosPorCnes,
                raioKm,
                MAX_HOSPITAIS
        );
    }

    private EncaminhamentoResponse buildResponse(
            String coIbge,
            String municipioOrigem,
            String tpLeito,
            List<HospitalDTO> hospitais,
            String pressaoSus
    ) {
        return EncaminhamentoResponse.builder()
                .coIbge(coIbge)
                .municipioOrigem(municipioOrigem)
                .tpLeito(tpLeito)
                .hospitais(hospitais)
                .pressaoSus(pressaoSus)
                .build();
    }

    public String resolverTpLeito(String gravidade) {
        if (gravidade == null) {
            return "74";
        }

        String valor = gravidade.strip().toLowerCase();
        if (valor.equals("grave") || valor.equals("critica") || valor.equals("cr\u00edtica")) {
            return "81";
        }
        return "74";
    }

    double haversine(double lat1, double lon1, double lat2, double lon2) {
        return calculadoraDistanciaGeografica.haversine(lat1, lon1, lat2, lon2);
    }

    private String calcularPressaoSus(String coMunicipio, int leitosSus) {
        int anoAtual = Year.now().getValue();
        List<CasoDengue> recentes = casoDenguePort.findByCoMunicipioAndAno(coMunicipio, anoAtual);

        if (recentes.isEmpty() || leitosSus == 0) {
            return "NORMAL";
        }

        long totalRecente = recentes.stream()
                .sorted(Comparator.comparingInt(CasoDengue::getSemanaEpi).reversed())
                .limit(4)
                .mapToLong(CasoDengue::getTotalCasos)
                .sum();

        return classificacaoPressaoSusPolicy.classificar(totalRecente, leitosSus);
    }

    public List<HospitalDTO> buscarHospitaisDasCapitais(String ufFiltro) {
        List<HospitalDTO> resultado = new ArrayList<>();

        for (String uf : UFS_BRASIL) {
            if (ufFiltro != null && !ufFiltro.isEmpty() && !uf.equals(ufFiltro)) {
                continue;
            }

            String codigoCapital = catalogoGeograficoBrasil.codigoCapitalEncaminhamento(uf);
            if (codigoCapital == null) {
                continue;
            }

            try {
                EncaminhamentoResponse resposta = buscarHospitais(codigoCapital, "74", 1);
                if (resposta != null && resposta.getHospitais() != null && !resposta.getHospitais().isEmpty()) {
                    resultado.add(resposta.getHospitais().get(0));
                }
            } catch (Exception ignored) {
                // Capital sem dados suficientes nao deve quebrar visoes agregadas.
            }
        }

        resultado.sort((primeiro, segundo) -> Integer.compare(segundo.getQtLeitosSus(), primeiro.getQtLeitosSus()));
        return resultado.stream().limit(5).collect(Collectors.toList());
    }
}
