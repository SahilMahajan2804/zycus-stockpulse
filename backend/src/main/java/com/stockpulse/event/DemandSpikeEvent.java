package com.stockpulse.event;

import com.stockpulse.domain.enums.TriggerReason;

public record DemandSpikeEvent(String productId, TriggerReason triggerReason) { }
