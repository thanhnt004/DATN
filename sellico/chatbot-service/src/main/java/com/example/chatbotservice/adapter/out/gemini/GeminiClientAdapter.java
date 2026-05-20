package com.example.chatbotservice.adapter.out.gemini;

import com.example.chatbotservice.application.port.out.GeminiClientPort;
import com.example.chatbotservice.domain.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Component
@Slf4j
public class GeminiClientAdapter implements GeminiClientPort {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiClientAdapter(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model:gemini-2.0-flash}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public String generateResponse(String systemInstruction, List<ChatMessage> conversationHistory, String userMessage) {
        Map<String, Object> requestBody = buildRequestBody(systemInstruction, conversationHistory, userMessage);

        String url = String.format("/models/%s:generateContent?key=%s", model, apiKey);

        log.debug("Calling Gemini API with model: {}", model);

        int maxRetries = 3;
        long delayMs = 2000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> response = webClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                return extractTextFromResponse(response);
            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("Gemini API rate limited (429), attempt {}/{}, retrying in {}ms...", attempt, maxRetries, delayMs);
                if (attempt == maxRetries) throw e;
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
                delayMs *= 2;
            }
        }
        return "Xin lỗi, mình không thể xử lý yêu cầu này lúc này.";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(String systemInstruction, List<ChatMessage> history, String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();

        // System instruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        // Contents: conversation history + current message
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            for (ChatMessage msg : history) {
                contents.add(Map.of(
                        "role", msg.getRole(),
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        // Current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        body.put("contents", contents);

        // Generation config
        body.put("generationConfig", Map.of(
                "temperature", 0.7,
                "topP", 0.9,
                "topK", 40,
                "maxOutputTokens", 1024
        ));

        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        if (response == null) {
            log.error("Gemini API returned null response");
            return "Xin lỗi, mình không thể xử lý yêu cầu này lúc này.";
        }

        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("No candidates in Gemini response");
                return "Xin lỗi, mình không thể xử lý yêu cầu này lúc này.";
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> part : parts) {
                if (part.containsKey("text")) {
                    sb.append(part.get("text"));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            return "Xin lỗi, mình không thể xử lý yêu cầu này lúc này.";
        }
    }
}
