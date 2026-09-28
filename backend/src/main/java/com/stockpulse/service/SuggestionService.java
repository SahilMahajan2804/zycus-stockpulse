package com.stockpulse.service;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.dto.response.PricingSuggestionResponse;
import com.stockpulse.dto.response.ReorderSuggestionResponse;
import com.stockpulse.exception.ProductNotFoundException;
import com.stockpulse.exception.SuggestionNotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderContext;
import com.stockpulse.strategy.ReorderResult;
import com.stockpulse.strategy.StrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuggestionService {
    private final ProductRepository products;
    private final PricingSuggestionRepository pricingSuggestions;
    private final ReorderSuggestionRepository reorderSuggestions;
    private final StrategyRegistry strategies;

    @Transactional(readOnly = true)
    public void validateProductExists(String productId) {
        if (!products.existsById(productId)) throw new ProductNotFoundException(productId);
    }

    @Transactional
    public void generate(String productId, TriggerReason reason) {
        Product product = products.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        String pricingKey = productId + ":" + reason + ":PRICING";
        String reorderKey = productId + ":" + reason + ":REORDER";
        boolean createPricing = !pricingSuggestions.existsByDedupeKey(pricingKey);
        boolean createReorder = !reorderSuggestions.existsByDedupeKey(reorderKey);
        if (!createPricing && !createReorder) return;

        Double average = products.averagePeerVelocity(product.getCategory(), productId);
        double categoryAverage = average == null ? 0.0 : average;
        PricingContext pricingContext = new PricingContext(product.getName(), product.getCategory(), product.getCurrentPrice(),
                product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAverage, reason);
        ReorderContext reorderContext = new ReorderContext(product.getName(), product.getCategory(), product.getCurrentPrice(),
                product.getStockLevel(), product.getReorderThreshold(), product.getDemandVelocity(), categoryAverage, reason);

        if (createPricing) {
                PricingResult result = strategies.activePricing().suggest(pricingContext);
                PricingSuggestion suggestion = new PricingSuggestion();
                suggestion.setProduct(product);
                suggestion.setCurrentPrice(product.getCurrentPrice());
                suggestion.setRecommendedPrice(result.recommendedPrice());
                suggestion.setDirection(result.direction());
                suggestion.setConfidence(result.confidence());
                suggestion.setReasoning(result.reasoning());
                suggestion.setStatus(SuggestionStatus.PENDING);
                suggestion.setTriggerReason(reason);
                suggestion.setSource(result.source());
                suggestion.setDedupeKey(pricingKey);
                pricingSuggestions.saveAndFlush(suggestion);
        }
        if (createReorder) {
                ReorderResult result = strategies.activeReorder().suggest(reorderContext);
                ReorderSuggestion suggestion = new ReorderSuggestion();
                suggestion.setProduct(product);
                suggestion.setCurrentStock(product.getStockLevel());
                suggestion.setRecommendedQuantity(result.recommendedQuantity());
                suggestion.setLeadTimeDays(result.leadTimeDays());
                suggestion.setConfidence(result.confidence());
                suggestion.setReasoning(result.reasoning());
                suggestion.setStatus(SuggestionStatus.PENDING);
                suggestion.setTriggerReason(reason);
                suggestion.setSource(result.source());
                suggestion.setDedupeKey(reorderKey);
                reorderSuggestions.saveAndFlush(suggestion);
        }
        boolean hasPendingPricing = pricingSuggestions.existsByProduct_IdAndStatus(productId, SuggestionStatus.PENDING);
        product.recomputeStatus(hasPendingPricing);
        products.save(product);
    }

    @Transactional
    public PricingSuggestionResponse decidePricing(UUID id, SuggestionStatus decision) {
        requireDecision(decision);
        PricingSuggestion suggestion = pricingSuggestions.findById(id).orElseThrow(() -> new SuggestionNotFoundException(id.toString()));
        if (decision == SuggestionStatus.ACCEPTED) suggestion.accept(); else suggestion.reject();
        boolean pending = pricingSuggestions.existsByProduct_IdAndStatus(
            suggestion.getProduct().getId(), SuggestionStatus.PENDING);
        suggestion.getProduct().recomputeStatus(pending);
        products.save(suggestion.getProduct());
        return PricingSuggestionResponse.from(pricingSuggestions.save(suggestion));
    }

    @Transactional
    public ReorderSuggestionResponse decideReorder(UUID id, SuggestionStatus decision) {
        requireDecision(decision);
        ReorderSuggestion suggestion = reorderSuggestions.findById(id).orElseThrow(() -> new SuggestionNotFoundException(id.toString()));
        if (decision == SuggestionStatus.ACCEPTED) suggestion.accept(); else suggestion.reject();
        boolean pending = pricingSuggestions.existsByProduct_IdAndStatus(
            suggestion.getProduct().getId(), SuggestionStatus.PENDING);
        suggestion.getProduct().recomputeStatus(pending);
        products.save(suggestion.getProduct());
        return ReorderSuggestionResponse.from(reorderSuggestions.save(suggestion));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> pricingList(String status, String productId, String triggerReason) {
        List<PricingSuggestion> items = pricingSuggestions.findAll().stream()
                .filter(s -> matches(s.getStatus(), s.getProduct().getId(), s.getTriggerReason(), status, productId, triggerReason))
                .sorted(Comparator.comparing(PricingSuggestion::getCreatedAt).reversed()).toList();
        return Map.of("items", items.stream().map(PricingSuggestionResponse::from).toList(), "total", items.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reorderList(String status, String productId, String triggerReason) {
        List<ReorderSuggestion> items = reorderSuggestions.findAll().stream()
                .filter(s -> matches(s.getStatus(), s.getProduct().getId(), s.getTriggerReason(), status, productId, triggerReason))
                .sorted(Comparator.comparing(ReorderSuggestion::getCreatedAt).reversed()).toList();
        return Map.of("items", items.stream().map(ReorderSuggestionResponse::from).toList(), "total", items.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> pendingRecommendations() {
        List<Map<String, Object>> items = new ArrayList<>();
        List<PricingSuggestion> pendingPricing = pricingSuggestions.findByStatus(SuggestionStatus.PENDING);
        List<ReorderSuggestion> pendingReorders = reorderSuggestions.findByStatus(SuggestionStatus.PENDING);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (PricingSuggestion s : pendingPricing) {
            String key = s.getProduct().getId() + ":" + s.getTriggerReason();
            Map<String, Object> item = pendingItem(byKey, key, s.getProduct(), s.getTriggerReason());
            item.put("pricingSuggestion", Map.of("id", s.getId(), "currentPrice", s.getCurrentPrice(),
                    "recommendedPrice", s.getRecommendedPrice(), "direction", s.getDirection(),
                    "confidence", s.getConfidence(), "reasoning", s.getReasoning(), "source", s.getSource()));
        }
        for (ReorderSuggestion s : pendingReorders) {
            String key = s.getProduct().getId() + ":" + s.getTriggerReason();
            Map<String, Object> item = pendingItem(byKey, key, s.getProduct(), s.getTriggerReason());
            item.put("reorderSuggestion", Map.of("id", s.getId(), "currentStock", s.getCurrentStock(),
                    "recommendedQuantity", s.getRecommendedQuantity(), "leadTimeDays", s.getLeadTimeDays(),
                    "confidence", s.getConfidence(), "reasoning", s.getReasoning(), "source", s.getSource()));
        }
        items.addAll(byKey.values());
        return Map.of("items", items);
    }

    private Map<String, Object> pendingItem(Map<String, Map<String, Object>> map, String key,
            Product product, TriggerReason reason) {
        return map.computeIfAbsent(key, ignored -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("triggerReason", reason);
            return item;
        });
    }

    private boolean matches(SuggestionStatus currentStatus, String currentProductId, TriggerReason currentReason,
            String status, String productId, String reason) {
        return (status == null || currentStatus.name().equalsIgnoreCase(status))
                && (productId == null || currentProductId.equals(productId))
                && (reason == null || currentReason.name().equalsIgnoreCase(reason));
    }

    private void requireDecision(SuggestionStatus status) {
        if (status != SuggestionStatus.ACCEPTED && status != SuggestionStatus.REJECTED)
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
    }
}
