package com.stockpulse.dto.request;

import com.stockpulse.domain.enums.Category;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product id is mandatory")
    private String id;

    @NotBlank(message = "SKU is mandatory")
    private String sku;

    @NotBlank(message = "Product name is mandatory")
    private String name;

    @NotNull(message = "Category is mandatory")
    private Category category;

    @NotNull(message = "Current price is mandatory")
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentPrice;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal costPrice;

    private String supplierId;

    @NotNull(message = "Stock level is mandatory")
    @Min(value = 0, message = "Stock level cannot be negative")
    private Integer stockLevel;

    @NotNull(message = "Reorder threshold is mandatory")
    @Min(value = 0, message = "Reorder threshold cannot be negative")
    private Integer reorderThreshold;

}
