package com.stockpulse.repository;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, UUID> {
    boolean existsByDedupeKey(String dedupeKey);
    boolean existsByProduct_IdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);
    List<ReorderSuggestion> findByStatus(SuggestionStatus status);
    long countByStatus(SuggestionStatus status);
}
