package br.com.fiap.vigisus.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiRequest {

    private String model;
    private List<OpenAiMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiMessage {

        private String role;
        private String content;
    }
}
