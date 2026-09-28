package com.stockpulse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.strategy.AiPricingStrategy;
import com.stockpulse.strategy.AiReorderStrategy;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.ReorderContext;

class LlmCallTests {
    @Test
    void aiPricingCallsTheLlmGateway() {
        LLMGateway gateway = mock(LLMGateway.class);
        when(gateway.callLLM(anyString())).thenReturn(
                "{\"recommendedPrice\":110,\"direction\":\"INCREASE\",\"confidence\":0.8,\"reasoning\":\"low stock\"}");
        AiPricingStrategy strategy = new AiPricingStrategy(gateway, new PromptBuilder(),
                new LlmResponseParser(new ObjectMapper()), new SuggestionValidator());

        strategy.suggest(new PricingContext("Test", Category.ELECTRONICS, new BigDecimal("100.00"),
                8, 15, 4, 7, TriggerReason.INVENTORY_LOW));

        verify(gateway).callLLM(anyString());
    }

    @Test
    void aiReorderCallsTheLlmGateway() {
        LLMGateway gateway = mock(LLMGateway.class);
        when(gateway.callLLM(anyString())).thenReturn(
                "{\"recommendedQuantity\":30,\"leadTimeDays\":5,\"confidence\":0.8,\"reasoning\":\"low stock\"}");
        AiReorderStrategy strategy = new AiReorderStrategy(gateway, new PromptBuilder(),
                new LlmResponseParser(new ObjectMapper()), new SuggestionValidator());

        strategy.suggest(new ReorderContext("Test", Category.ELECTRONICS, new BigDecimal("100.00"),
                8, 15, 4, 7, TriggerReason.INVENTORY_LOW));

        verify(gateway).callLLM(anyString());
    }
}
