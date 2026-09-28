package com.stockpulse.strategy;

import org.springframework.stereotype.Component;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;

import lombok.RequiredArgsConstructor;

@Component("pricingAi")
@RequiredArgsConstructor
public class AiPricingStrategy implements PricingStrategy {
    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;

    @Override
    public PricingResult suggest(PricingContext context) {
        return validator.pricing(parser.parse(gateway.callLLM(prompts.pricing(context))), context);
    }
}
