package com.stockpulse.ai;

import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.ReorderContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class PromptBuilder {
    public String pricing(PricingContext context) {
        String name = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "prompts/pricing-low-stock.txt";
            case DEMAND_SPIKE -> "prompts/pricing-spike.txt";
            default -> "prompts/pricing-manual.txt";
        };
        return fill(name, context.productName(), context.category().name(), context.currentPrice().toPlainString(),
                context.stock(), context.reorderThreshold(), context.demandVelocity(), context.categoryAverageVelocity(), context.triggerReason());
    }

    public String reorder(ReorderContext context) {
        String name = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "prompts/reorder-low-stock.txt";
            case DEMAND_SPIKE -> "prompts/reorder-spike.txt";
            default -> "prompts/reorder-manual.txt";
        };
        return fill(name, context.productName(), context.category().name(), context.currentPrice().toPlainString(),
                context.stock(), context.reorderThreshold(), context.demandVelocity(), context.categoryAverageVelocity(), context.triggerReason());
    }

    private String fill(String resource, String product, String category, String price, int stock,
            int threshold, int velocity, double average, TriggerReason trigger) {
        try {
            String template = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
            return template.replace("{{productName}}", product).replace("{{category}}", category)
                    .replace("{{currentPrice}}", price).replace("{{stock}}", String.valueOf(stock))
                    .replace("{{threshold}}", String.valueOf(threshold)).replace("{{velocity}}", String.valueOf(velocity))
                    .replace("{{categoryAverage}}", String.valueOf(average)).replace("{{trigger}}", trigger.name());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load prompt: " + resource, e);
        }
    }
}
