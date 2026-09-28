package com.stockpulse.strategy;

import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("pricingRuleBased")
public class RuleBasedPricingStrategy implements PricingStrategy {
    @Override
    public PricingResult suggest(PricingContext context) {
        BigDecimal multiplier;
        Direction direction;
        String reasoning;
        if (context.stock() < context.reorderThreshold()) {
            multiplier = new BigDecimal("1.10");
            direction = Direction.INCREASE;
            reasoning = "Stock is below the reorder threshold; a modest increase protects inventory until replenishment.";
        } else if (context.demandVelocity() > 2 * context.categoryAverageVelocity()) {
            multiplier = new BigDecimal("1.05");
            direction = Direction.INCREASE;
            reasoning = "Demand velocity is more than twice the peer category average.";
        } else {
            multiplier = BigDecimal.ONE;
            direction = Direction.HOLD;
            reasoning = "Stock and demand do not indicate a price change.";
        }
        return new PricingResult(context.currentPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP),
                direction, new BigDecimal("0.70"), reasoning, SuggestionSource.RULE_BASED);
    }
}
