package br.com.fiap.vigisus.dto.openai;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OpenAiResponse {

    private List<Choice> choices;

    @Data
    @NoArgsConstructor
    public static class Choice {

        private Message message;
    }

    @Data
    @NoArgsConstructor
    public static class Message {

        private String role;
        private String content;
    }
}
