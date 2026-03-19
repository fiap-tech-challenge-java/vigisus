package br.com.fiap.vigisus.dto.openai;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiMessage {

        private String role;
        private String content;
    }
}
