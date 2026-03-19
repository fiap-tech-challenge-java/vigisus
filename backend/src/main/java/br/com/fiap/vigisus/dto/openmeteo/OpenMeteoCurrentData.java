package br.com.fiap.vigisus.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenMeteoCurrentData {

    @JsonProperty("temperature_2m")
    private Double temperature2m;

    @JsonProperty("relative_humidity_2m")
    private Integer relativeHumidity2m;

    @JsonProperty("precipitation")
    private Double precipitation;
}
