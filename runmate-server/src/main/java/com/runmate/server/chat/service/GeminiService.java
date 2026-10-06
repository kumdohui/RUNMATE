package com.runmate.server.chat.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.runmate.server.chat.domain.ChatMessage;
import com.runmate.server.chat.exception.ChatException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GeminiService {
    private static final String SYSTEM_PROMPT = """
            당신은 RUNMATE의 러닝 도우미입니다.
            한국어로 친절하고 간결하게 답변하세요.
            답변 가능한 범위는 러닝, 조깅, 달리기 훈련, 페이스, 회복,
            러닝과 관련된 식사·수분 섭취, 부상 예방, 러닝 코스와 날씨입니다.
            사용자의 마지막 질문이 답변의 최우선 주제입니다.
            최근 러닝 기록은 질문에 도움이 될 때만 보조 근거로 짧게 활용하고,
            기록을 칭찬하거나 요약하는 말로 답변을 시작하지 마세요.
            질문에 대한 결론을 먼저 말한 뒤 구체적인 실천 방법을 설명하세요.
            과장된 칭찬이나 어색한 감탄은 피하고 자연스러운 존댓말을 사용하세요.
            짧은 질문에도 문장이 중간에 끊기지 않도록 완결된 답변을 작성하세요.
            범위를 벗어난 질문에는 내용을 설명하지 말고
            "저는 러닝 관련 질문만 답변할 수 있어요."라고 안내하세요.
            의료 진단을 확정하지 말고 통증이나 부상 질문에는 전문가 상담을 권하세요.
            사용자가 제공하지 않은 개인정보를 추측하지 마세요.
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Qualifier("geminiRestClient") RestClient restClient,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String answer(
            List<ChatMessage> history,
            String question,
            String runningRecordContext
    ) {
        requireApiKey();

        List<GeminiContent> contents = new ArrayList<>();
        history.forEach(message -> contents.add(toGeminiContent(message)));
        String contextualQuestion = runningRecordContext == null
                ? question
                : "사용자의 핵심 질문: " + question
                        + "\n\n참고 가능한 최근 러닝 기록:\n"
                        + runningRecordContext;
        contents.add(new GeminiContent(
                "user",
                List.of(new GeminiPart(contextualQuestion))
        ));

        GeminiRequest request = new GeminiRequest(
                new GeminiContent("user", List.of(new GeminiPart(SYSTEM_PROMPT))),
                contents,
                new GenerationConfig(2048, 0.4)
        );

        try {
            GeminiResponse response = restClient
                    .post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            String answer = response == null ? null : response.text();
            if (answer == null || answer.isBlank()) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 답변을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요."
                );
            }
            return answer.trim();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new ChatException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "현재 AI 요청이 많습니다. 잠시 후 다시 시도해 주세요."
                );
            }
            throw new ChatException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    public String modelName() {
        return model;
    }

    private GeminiContent toGeminiContent(ChatMessage message) {
        String role = "ASSISTANT".equals(message.getRole()) ? "model" : "user";
        return new GeminiContent(
                role,
                List.of(new GeminiPart(message.getContent()))
        );
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서비스 설정이 완료되지 않았습니다."
            );
        }
    }

    private record GeminiRequest(
            @JsonProperty("system_instruction") GeminiContent systemInstruction,
            List<GeminiContent> contents,
            @JsonProperty("generationConfig") GenerationConfig generationConfig
    ) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GenerationConfig(
            int maxOutputTokens,
            double temperature
    ) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
        String text() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            GeminiContent content = candidates.get(0).content();
            if (content == null || content.parts() == null) {
                return null;
            }
            return content.parts()
                    .stream()
                    .map(GeminiPart::text)
                    .filter(value -> value != null && !value.isBlank())
                    .reduce("", String::concat);
        }
    }

    private record Candidate(GeminiContent content) {
    }
}
