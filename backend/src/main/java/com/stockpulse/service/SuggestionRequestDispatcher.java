package com.stockpulse.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.stockpulse.domain.enums.TriggerReason;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuggestionRequestDispatcher {
    private final SuggestionService suggestionService;

    @Async("advisorExecutor")
    public void generate(String productId, TriggerReason reason) {
        try {
            suggestionService.generateAiSuggestions(productId, reason);
            suggestionService.generateRuleBasedSuggestions(productId, reason);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Concurrent duplicate suggestion skipped for {} / {}", productId, reason);
        } catch (Exception exception) {
            log.error("Manual suggestion generation failed for {} / {}", productId, reason, exception);
        }
    }
}
