package br.com.fiap.vigisus.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenMeteoResponse {

    @JsonProperty("current")
    private OpenMeteoCurrentData current;

    @JsonProperty("daily")
    private OpenMeteoDailyData daily;
}
