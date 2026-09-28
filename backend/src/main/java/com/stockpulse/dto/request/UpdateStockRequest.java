package com.stockpulse.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateStockRequest {

    @NotNull(message = "Stock level is mandatory")
    @Min(value = 0, message = "Stock level cannot be negative")
    private Integer stockLevel;
}
