package com.stockpulse.event;

import com.stockpulse.domain.enums.TriggerReason;

public record InventoryLowEvent(String productId, TriggerReason triggerReason) { }
