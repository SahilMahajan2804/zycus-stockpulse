package com.stockpulse.dto.response;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReorderSuggestionResponse(UUID id, String productId, Integer currentStock,
        Integer recommendedQuantity, Integer leadTimeDays, BigDecimal confidence, String reasoning,
        SuggestionStatus status, TriggerReason triggerReason, SuggestionSource generationSource,
        LocalDateTime createdAt) {
    public static ReorderSuggestionResponse from(ReorderSuggestion s) {
        return new ReorderSuggestionResponse(s.getId(), s.getProduct().getId(), s.getCurrentStock(),
                s.getRecommendedQuantity(), s.getLeadTimeDays(), s.getConfidence(), s.getReasoning(),
                s.getStatus(), s.getTriggerReason(), s.getSource(), s.getCreatedAt());
    }
}
