package com.stockpulse.strategy;

import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import java.math.BigDecimal;

public record PricingResult(BigDecimal recommendedPrice, Direction direction,
        BigDecimal confidence, String reasoning, SuggestionSource source) { }
