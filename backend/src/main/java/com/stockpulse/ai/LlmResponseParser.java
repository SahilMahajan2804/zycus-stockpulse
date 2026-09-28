package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LlmResponseParser {
    private final ObjectMapper mapper;

    public JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Empty LLM response");
        String cleaned = raw.trim().replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalArgumentException("LLM response did not contain a JSON object");
        try { return mapper.readTree(cleaned.substring(start, end + 1)); }
        catch (Exception e) { throw new IllegalArgumentException("Malformed LLM JSON", e); }
    }
}
