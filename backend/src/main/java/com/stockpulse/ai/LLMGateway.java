package com.stockpulse.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LLMGateway {
    private final RestClient restClient;
    private final String provider;
    private final String model;
    private final String apiKey;
    private final String baseUrl;

    public LLMGateway(RestClient.Builder builder,
            @Value("${llm.provider:gemini}") String provider,
            @Value("${llm.model:gemini-3.8-flash}") String model,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.restClient = builder.build();
        this.provider = provider;
        this.model = model;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String callLLM(String prompt) {
        boolean keyConfigured = apiKey != null && !apiKey.isBlank();
        log.info("LLM request started: provider={}, model={}, baseUrl={}, apiKeyConfigured={}",
            provider, model, baseUrl, keyConfigured);
        if (!keyConfigured) throw new IllegalStateException("LLM API key is not configured");
        if (provider.equalsIgnoreCase("gemini")) return callGemini(prompt);
        JsonNode response = restClient.post().uri(baseUrl + "/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "temperature", 0.2,
                        "messages", List.of(Map.of("role", "user", "content", prompt))))
                .retrieve().body(JsonNode.class);
        JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
        if (content == null || content.isMissingNode() || content.isNull()) throw new IllegalStateException("LLM response had no content");
        log.info("LLM request completed: provider={}, model={}", provider, model);
        return content.asText();
    }

    private String callGemini(String prompt) {
        String endpoint = baseUrl.replaceAll("/$", "") + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        JsonNode response = restClient.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                .retrieve().body(JsonNode.class);
        JsonNode text = response == null ? null : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || text.isMissingNode() || text.isNull()) throw new IllegalStateException("Gemini response had no content");
        log.info("LLM request completed: provider={}, model={}", provider, model);
        return text.asText();
    }
}
