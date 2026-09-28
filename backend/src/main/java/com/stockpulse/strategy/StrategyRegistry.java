package com.stockpulse.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class StrategyRegistry {
    private final Map<String, PricingStrategy> pricingStrategies;
    private final Map<String, ReorderStrategy> reorderStrategies;
    private final AtomicReference<String> activePricing;
    private final AtomicReference<String> activeReorder;

    public StrategyRegistry(Map<String, PricingStrategy> pricingStrategies,
            Map<String, ReorderStrategy> reorderStrategies,
            @Value("${stockpulse.strategy.pricing:ai}") String pricing,
            @Value("${stockpulse.strategy.reorder:ruleBased}") String reorder) {
        this.pricingStrategies = pricingStrategies;
        this.reorderStrategies = reorderStrategies;
        this.activePricing = new AtomicReference<>(normalizeConfigured(pricing));
        this.activeReorder = new AtomicReference<>(normalizeConfigured(reorder));
    }

    public PricingStrategy activePricing() { return pricingStrategies.get(resolve(activePricing.get(), "pricing")); }
    public ReorderStrategy activeReorder() { return reorderStrategies.get(resolve(activeReorder.get(), "reorder")); }
    public String activePricingName() { return activePricing.get(); }
    public String activeReorderName() { return activeReorder.get(); }

    public void switchPricing(String strategy) {
        String value = normalize(strategy);
        resolve(value, "pricing");
        activePricing.set(value);
    }

    public void switchReorder(String strategy) {
        String value = normalize(strategy);
        resolve(value, "reorder");
        activeReorder.set(value);
    }

    private String resolve(String value, String type) {
        String key = value.equals("AI") ? (type.equals("pricing") ? "pricingAi" : "reorderAi") :
                value.equals("RULE") ? (type.equals("pricing") ? "pricingRuleBased" : "reorderRuleBased") : value;
        boolean exists = type.equals("pricing") ? pricingStrategies.containsKey(key) : reorderStrategies.containsKey(key);
        if (!exists) throw new IllegalArgumentException("Unknown " + type + " strategy: " + value);
        return key;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Strategy is required");
        String normalized = value.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.equals("AI") || upper.equals("RULE")) return upper;
        throw new IllegalArgumentException("Unknown strategy: " + value);
    }

    private String normalizeConfigured(String value) {
        if (value != null && value.equalsIgnoreCase("ruleBased")) return "RULE";
        return normalize(value);
    }
}
