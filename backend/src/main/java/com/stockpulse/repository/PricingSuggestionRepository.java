package com.stockpulse.repository;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, UUID> {
    boolean existsByDedupeKey(String dedupeKey);
    boolean existsByProduct_IdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);
    boolean existsByProduct_IdAndStatus(String productId, SuggestionStatus status);
    long countByStatus(SuggestionStatus status);
    List<PricingSuggestion> findByStatus(SuggestionStatus status);
}
