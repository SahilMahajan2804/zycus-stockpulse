package com.stockpulse.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;
}
