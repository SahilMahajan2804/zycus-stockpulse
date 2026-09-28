package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderContext;
import com.stockpulse.strategy.ReorderResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class SuggestionValidator {
    public PricingResult pricing(JsonNode json, PricingContext context) {
        BigDecimal price = decimal(json, "recommendedPrice");
        BigDecimal confidence = decimal(json, "confidence");
        BigDecimal current = context.currentPrice();
        if (price.signum() <= 0 || price.compareTo(current.multiply(new BigDecimal("0.5"))) < 0 ||
                price.compareTo(current.multiply(new BigDecimal("1.5"))) > 0) throw new IllegalArgumentException("Recommended price outside allowed range");
        validateConfidence(confidence);
        Direction actual = price.compareTo(current) > 0 ? Direction.INCREASE : price.compareTo(current) < 0 ? Direction.DECREASE : Direction.HOLD;
        if (!json.path("direction").asText().equalsIgnoreCase(actual.name())) throw new IllegalArgumentException("Direction does not match price change");
        return new PricingResult(price, actual, confidence, text(json, "reasoning"), SuggestionSource.AI);
    }

    public ReorderResult reorder(JsonNode json, ReorderContext context) {
        int quantity = json.path("recommendedQuantity").asInt(-1);
        BigDecimal confidence = decimal(json, "confidence");
        if (quantity <= 0 || quantity > 10 * context.reorderThreshold()) throw new IllegalArgumentException("Recommended quantity outside allowed range");
        validateConfidence(confidence);
        int lead = json.path("leadTimeDays").asInt(5);
        if (lead <= 0) lead = 5;
        return new ReorderResult(quantity, lead, confidence, text(json, "reasoning"), SuggestionSource.AI);
    }

    private BigDecimal decimal(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isNumber()) throw new IllegalArgumentException("Missing numeric field: " + field);
        return node.decimalValue();
    }
    private String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) throw new IllegalArgumentException("Missing text field: " + field);
        return node.asText();
    }
    private void validateConfidence(BigDecimal confidence) {
        if (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("Confidence must be between zero and one");
    }
}
