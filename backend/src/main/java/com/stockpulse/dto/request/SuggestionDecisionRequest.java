package com.stockpulse.dto.request;

import com.stockpulse.domain.enums.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record SuggestionDecisionRequest(@NotNull SuggestionStatus status) { }
