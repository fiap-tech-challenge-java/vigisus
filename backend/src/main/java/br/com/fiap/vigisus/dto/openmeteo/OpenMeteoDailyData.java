package br.com.fiap.vigisus.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OpenMeteoDailyData {

    @JsonProperty("time")
    private List<String> time;

    @JsonProperty("temperature_2m_max")
    private List<Double> temperature2mMax;

    @JsonProperty("precipitation_sum")
    private List<Double> precipitationSum;

    @JsonProperty("precipitation_probability_max")
    private List<Integer> precipitationProbabilityMax;
}
