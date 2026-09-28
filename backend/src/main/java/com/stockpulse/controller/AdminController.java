package com.stockpulse.controller;

import com.stockpulse.dto.request.StrategySwitchRequest;
import com.stockpulse.strategy.StrategyRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final StrategyRegistry strategies;

    @GetMapping("/strategy")
    public Map<String, String> active() {
        return Map.of("pricing", strategies.activePricingName(), "reorder", strategies.activeReorderName());
    }

    @PutMapping("/strategy")
    public Map<String, String> switchStrategy(@Valid @RequestBody StrategySwitchRequest request) {
        strategies.switchPricing(request.strategy());
        strategies.switchReorder(request.strategy());
        return active();
    }
}
