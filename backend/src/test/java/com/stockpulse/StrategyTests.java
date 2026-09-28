package com.stockpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.strategy.AiPricingStrategy;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderContext;
import com.stockpulse.strategy.ReorderResult;
import com.stockpulse.strategy.RuleBasedPricingStrategy;
import com.stockpulse.strategy.RuleBasedReorderStrategy;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StrategyTests {
    private final RuleBasedPricingStrategy pricing = new RuleBasedPricingStrategy();
    private final RuleBasedReorderStrategy reorder = new RuleBasedReorderStrategy();

    @Test
    void pricingRulesCoverLowStockSpikeAndHold() {
        PricingResult low = pricing.suggest(context(8, 15, 4, 7));
        assertEquals(new BigDecimal("110.00"), low.recommendedPrice());
        assertEquals(Direction.INCREASE, low.direction());
        assertEquals(SuggestionSource.RULE_BASED, low.source());

        PricingResult spike = pricing.suggest(context(40, 15, 15, 7));
        assertEquals(new BigDecimal("105.00"), spike.recommendedPrice());

        PricingResult hold = pricing.suggest(context(40, 15, 3, 7));
        assertEquals(new BigDecimal("100.00"), hold.recommendedPrice());
        assertEquals(Direction.HOLD, hold.direction());
    }

    @Test
    void reorderUsesThreeThresholdTarget() {
        ReorderContext context = new ReorderContext("Hoodie", Category.APPAREL, new BigDecimal("54.99"),
                8, 15, 12, 7, TriggerReason.INVENTORY_LOW);
        ReorderResult result = reorder.suggest(context);
        assertEquals(37, result.recommendedQuantity());
        assertEquals(SuggestionSource.RULE_BASED, result.source());
    }

    @Test
    void aiPricingUsesRulesWhenGatewayFails() {
        LLMGateway gateway = mock(LLMGateway.class);
        when(gateway.callLLM(anyString())).thenThrow(new IllegalStateException("timeout"));
        AiPricingStrategy ai = new AiPricingStrategy(gateway, new PromptBuilder(),
                new LlmResponseParser(new ObjectMapper()), new SuggestionValidator(), pricing);
        PricingResult result = ai.suggest(context(8, 15, 4, 7));
        assertEquals(SuggestionSource.RULE_FALLBACK, result.source());
        assertEquals(new BigDecimal("110.00"), result.recommendedPrice());
    }

    @Test
    void responseParserExtractsFencedJsonAndValidatorChecksPrice() {
        LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());
        var json = parser.parse("Here is the answer:\n```json\n{\"recommendedPrice\":110,\"direction\":\"INCREASE\",\"confidence\":0.8,\"reasoning\":\"low stock\",\"extra\":true}\n```\n");
        PricingResult result = new SuggestionValidator().pricing(json, context(8, 15, 4, 7));
        assertEquals(SuggestionSource.AI, result.source());
        assertThrows(IllegalArgumentException.class, () -> new SuggestionValidator().pricing(
                parser.parse("{\"recommendedPrice\":200,\"direction\":\"INCREASE\",\"confidence\":0.8,\"reasoning\":\"too high\"}"),
                context(8, 15, 4, 7)));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not json"));
    }

    private PricingContext context(int stock, int threshold, int velocity, double average) {
        return new PricingContext("Test", Category.ELECTRONICS, new BigDecimal("100.00"), stock,
                threshold, velocity, average, TriggerReason.INVENTORY_LOW);
    }
}
