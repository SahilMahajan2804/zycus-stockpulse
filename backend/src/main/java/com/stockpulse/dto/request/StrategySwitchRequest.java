package com.stockpulse.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StrategySwitchRequest(@NotBlank String strategy) { }
