package com.stockpulse.controller;

import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.dto.request.ManualSuggestionRequest;
import com.stockpulse.dto.request.SuggestionDecisionRequest;
import com.stockpulse.service.SuggestionService;
import com.stockpulse.service.SuggestionRequestDispatcher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestions;
    private final SuggestionRequestDispatcher dispatcher;

    @PostMapping("/products/{productId}/suggest-pricing")
    public ResponseEntity<Map<String, Object>> suggestPricing(@PathVariable String productId,
            @RequestBody(required = false) ManualSuggestionRequest request) {
        TriggerReason reason = request == null || request.triggerReason() == null ? TriggerReason.MANUAL : request.triggerReason();
        suggestions.validateProductExists(productId);
        dispatcher.generate(productId, reason);
        return ResponseEntity.accepted().body(Map.of("message", "Pricing recommendation generation started",
                "productId", productId, "triggerReason", reason));
    }

    @PostMapping("/products/{productId}/suggest-reorder")
    public ResponseEntity<Map<String, Object>> suggestReorder(@PathVariable String productId,
            @RequestBody(required = false) ManualSuggestionRequest request) {
        TriggerReason reason = request == null || request.triggerReason() == null ? TriggerReason.MANUAL : request.triggerReason();
        suggestions.validateProductExists(productId);
        dispatcher.generate(productId, reason);
        return ResponseEntity.accepted().body(Map.of("message", "Reorder recommendation generation started",
                "productId", productId, "triggerReason", reason));
    }

    @GetMapping("/pricing-suggestions")
    public Map<String, Object> pricing(@RequestParam(required = false) String status,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String triggerReason) {
        return suggestions.pricingList(status, productId, triggerReason);
    }

    @PatchMapping("/pricing-suggestions/{suggestionId}")
    public ResponseEntity<?> decidePricing(@PathVariable UUID suggestionId,
            @Valid @RequestBody SuggestionDecisionRequest request) {
        return ResponseEntity.ok(suggestions.decidePricing(suggestionId, request.status()));
    }

    @GetMapping("/reorder-suggestions")
    public Map<String, Object> reorder(@RequestParam(required = false) String status,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String triggerReason) {
        return suggestions.reorderList(status, productId, triggerReason);
    }

    @PatchMapping("/reorder-suggestions/{suggestionId}")
    public ResponseEntity<?> decideReorder(@PathVariable UUID suggestionId,
            @Valid @RequestBody SuggestionDecisionRequest request) {
        return ResponseEntity.ok(suggestions.decideReorder(suggestionId, request.status()));
    }

    @GetMapping("/recommendations/pending")
    public Map<String, Object> pending() { return suggestions.pendingRecommendations(); }
}
