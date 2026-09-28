package com.stockpulse.strategy;

import com.stockpulse.domain.enums.SuggestionSource;
import java.math.BigDecimal;

public record ReorderResult(int recommendedQuantity, int leadTimeDays,
        BigDecimal confidence, String reasoning, SuggestionSource source) { }
