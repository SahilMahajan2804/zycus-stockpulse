package com.stockpulse.strategy;

import com.stockpulse.domain.enums.SuggestionSource;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("reorderRuleBased")
public class RuleBasedReorderStrategy implements ReorderStrategy {
    @Override
    public ReorderResult suggest(ReorderContext context) {
        int quantity = Math.max(1, context.reorderThreshold() * 3 - context.stock());
        return new ReorderResult(quantity, 5, new BigDecimal("0.70"),
                "Replenish toward a three-times-threshold safety stock while accounting for current inventory.",
                SuggestionSource.RULE_BASED);
    }
}
