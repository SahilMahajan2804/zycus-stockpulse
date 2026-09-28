package com.stockpulse.strategy;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;
import com.stockpulse.domain.enums.SuggestionSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("pricingAi")
@RequiredArgsConstructor
@Slf4j
public class AiPricingStrategy implements PricingStrategy {
    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;
    private final RuleBasedPricingStrategy fallback;

    @Override
    public PricingResult suggest(PricingContext context) {
        try {
            return validator.pricing(parser.parse(gateway.callLLM(prompts.pricing(context))), context);
        } catch (Exception ex) {
            log.warn("AI pricing failed for {} ({}); using rule fallback", context.productName(), ex.toString());
            PricingResult result = fallback.suggest(context);
            return new PricingResult(result.recommendedPrice(), result.direction(), result.confidence(),
                    "AI unavailable; " + result.reasoning(), SuggestionSource.RULE_FALLBACK);
        }
    }
}
