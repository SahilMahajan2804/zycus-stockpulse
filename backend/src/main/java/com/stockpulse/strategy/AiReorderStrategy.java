package com.stockpulse.strategy;

import org.springframework.stereotype.Component;

import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.LlmResponseParser;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.ai.SuggestionValidator;

import lombok.RequiredArgsConstructor;

@Component("reorderAi")
@RequiredArgsConstructor
public class AiReorderStrategy implements ReorderStrategy {
    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;

    @Override
    public ReorderResult suggest(ReorderContext context) {
        return validator.reorder(parser.parse(gateway.callLLM(prompts.reorder(context))), context);
    }
}
