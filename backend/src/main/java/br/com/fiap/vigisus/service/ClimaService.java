package br.com.fiap.vigisus.service;

import br.com.fiap.vigisus.application.port.ClimaPort;
import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.openmeteo.OpenMeteoDailyData;
import br.com.fiap.vigisus.dto.openmeteo.OpenMeteoResponse;
import br.com.fiap.vigisus.exception.ApiExternaException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClimaService implements ClimaPort {

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestTemplate restTemplate;

    @Override
    @Cacheable(value = "clima-atual", key = "#lat + ',' + #lon")
    public ClimaAtualDTO buscarClimaAtual(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(OPEN_METEO_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation")
                .toUriString();

        OpenMeteoResponse response = restTemplate.getForObject(url, OpenMeteoResponse.class);

        if (response == null || response.getCurrent() == null) {
            throw new ApiExternaException("Open-Meteo");
        }

        return ClimaAtualDTO.builder()
                .temperatura(response.getCurrent().getTemperature2m())
                .umidade(response.getCurrent().getRelativeHumidity2m())
                .precipitacao(response.getCurrent().getPrecipitation())
                .build();
    }

    @Override
    public List<PrevisaoDiariaDTO> buscarPrevisao16Dias(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(OPEN_METEO_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("daily",
                        "temperature_2m_max,precipitation_sum,precipitation_probability_max")
                .queryParam("forecast_days", 16)
                .queryParam("timezone", "America/Sao_Paulo")
                .toUriString();

        OpenMeteoResponse response = restTemplate.getForObject(url, OpenMeteoResponse.class);

        if (response == null || response.getDaily() == null) {
            throw new ApiExternaException("Open-Meteo");
        }

        OpenMeteoDailyData daily = response.getDaily();
        List<PrevisaoDiariaDTO> previsoes = new ArrayList<>();

        for (int i = 0; i < daily.getTime().size(); i++) {
            Double tempMax = getOrNull(daily.getTemperature2mMax(), i);
            Double precip = getOrNull(daily.getPrecipitationSum(), i);
            Integer probChuva = getOrNull(daily.getPrecipitationProbabilityMax(), i);

            previsoes.add(PrevisaoDiariaDTO.builder()
                    .data(daily.getTime().get(i))
                    .temperaturaMaxima(tempMax)
                    .precipitacaoTotal(precip)
                    .probabilidadeChuva(probChuva)
                    .build());
        }

        return previsoes;
    }

    private <T> T getOrNull(List<T> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }
}
