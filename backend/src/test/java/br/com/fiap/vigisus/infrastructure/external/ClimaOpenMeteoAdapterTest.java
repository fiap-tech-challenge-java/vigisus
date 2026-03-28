package br.com.fiap.vigisus.infrastructure.external;

import br.com.fiap.vigisus.dto.ClimaAtualDTO;
import br.com.fiap.vigisus.dto.PrevisaoDiariaDTO;
import br.com.fiap.vigisus.dto.openmeteo.OpenMeteoCurrentData;
import br.com.fiap.vigisus.dto.openmeteo.OpenMeteoDailyData;
import br.com.fiap.vigisus.dto.openmeteo.OpenMeteoResponse;
import br.com.fiap.vigisus.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClimaOpenMeteoAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private ClimaOpenMeteoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ClimaOpenMeteoAdapter(restTemplate);
    }

    @Test
    void buscarClimaAtual_retornaDadosMapeados() {
        OpenMeteoCurrentData current = new OpenMeteoCurrentData();
        current.setTemperature2m(28.5);
        current.setRelativeHumidity2m(81);
        current.setPrecipitation(5.2);

        OpenMeteoResponse response = new OpenMeteoResponse();
        response.setCurrent(current);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class))).thenReturn(response);

        ClimaAtualDTO dto = adapter.buscarClimaAtual(-21.2, -45.0);

        assertThat(dto.getTemperatura()).isEqualTo(28.5);
        assertThat(dto.getUmidade()).isEqualTo(81);
        assertThat(dto.getPrecipitacao()).isEqualTo(5.2);
    }

    @Test
    void buscarClimaAtual_lancaQuandoRespostaInvalida() {
        when(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class))).thenReturn(new OpenMeteoResponse());

        assertThatThrownBy(() -> adapter.buscarClimaAtual(-21.2, -45.0))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Open-Meteo");
    }

    @Test
    void buscarPrevisao16Dias_retornaListaComCamposOpcionais() {
        OpenMeteoDailyData daily = new OpenMeteoDailyData();
        daily.setTime(List.of("2024-01-01", "2024-01-02"));
        daily.setTemperature2mMax(List.of(30.0));
        daily.setPrecipitationSum(List.of(10.0, 12.0));
        daily.setPrecipitationProbabilityMax(List.of(70));

        OpenMeteoResponse response = new OpenMeteoResponse();
        response.setDaily(daily);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class))).thenReturn(response);

        List<PrevisaoDiariaDTO> previsoes = adapter.buscarPrevisao16Dias(-21.2, -45.0);

        assertThat(previsoes).hasSize(2);
        assertThat(previsoes.get(0).getTemperaturaMaxima()).isEqualTo(30.0);
        assertThat(previsoes.get(0).getProbabilidadeChuva()).isEqualTo(70);
        assertThat(previsoes.get(1).getTemperaturaMaxima()).isNull();
        assertThat(previsoes.get(1).getProbabilidadeChuva()).isNull();
        assertThat(previsoes.get(1).getPrecipitacaoTotal()).isEqualTo(12.0);
    }

    @Test
    void buscarPrevisao16Dias_lancaQuandoRespostaInvalida() {
        when(restTemplate.getForObject(anyString(), eq(OpenMeteoResponse.class))).thenReturn(new OpenMeteoResponse());

        assertThatThrownBy(() -> adapter.buscarPrevisao16Dias(-21.2, -45.0))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Open-Meteo");
    }
}
