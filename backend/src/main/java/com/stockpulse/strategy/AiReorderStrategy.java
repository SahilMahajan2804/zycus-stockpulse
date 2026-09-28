package com.stockpulse.strategy;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;
import com.stockpulse.domain.enums.SuggestionSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("reorderAi")
@RequiredArgsConstructor
@Slf4j
public class AiReorderStrategy implements ReorderStrategy {
    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;
    private final RuleBasedReorderStrategy fallback;

    @Override
    public ReorderResult suggest(ReorderContext context) {
        try {
            return validator.reorder(parser.parse(gateway.callLLM(prompts.reorder(context))), context);
        } catch (Exception ex) {
            log.warn("AI reorder failed for {} ({}); using rule fallback", context.productName(), ex.toString());
            ReorderResult result = fallback.suggest(context);
            return new ReorderResult(result.recommendedQuantity(), result.leadTimeDays(), result.confidence(),
                    "AI unavailable; " + result.reasoning(), SuggestionSource.RULE_FALLBACK);
        }
    }
}
