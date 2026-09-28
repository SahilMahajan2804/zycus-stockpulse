package com.stockpulse.dto.request;

import com.stockpulse.domain.enums.TriggerReason;

public record ManualSuggestionRequest(TriggerReason triggerReason) { }
