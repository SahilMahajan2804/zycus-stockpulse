package com.stockpulse.dto.response;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PricingSuggestionResponse(UUID id, String productId, BigDecimal currentPrice,
        BigDecimal recommendedPrice, Direction direction, BigDecimal confidence, String reasoning,
        SuggestionStatus status, TriggerReason triggerReason, SuggestionSource generationSource,
        LocalDateTime createdAt) {
    public static PricingSuggestionResponse from(PricingSuggestion s) {
        return new PricingSuggestionResponse(s.getId(), s.getProduct().getId(), s.getCurrentPrice(),
                s.getRecommendedPrice(), s.getDirection(), s.getConfidence(), s.getReasoning(),
                s.getStatus(), s.getTriggerReason(), s.getSource(), s.getCreatedAt());
    }
}
