package com.stockpulse.strategy;

import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.TriggerReason;
import java.math.BigDecimal;

public record ReorderContext(String productName, Category category, BigDecimal currentPrice,
        int stock, int reorderThreshold, int demandVelocity, double categoryAverageVelocity,
        TriggerReason triggerReason) { }
