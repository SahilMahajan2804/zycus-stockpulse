package com.stockpulse.dto.response;

import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.Product;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ProductResponse {
    private String id;
    private String sku;
    private String name;
    private Category category;
    private BigDecimal currentPrice;
    private BigDecimal costPrice;
    private String supplierId;
    private Integer stockLevel;
    private Integer reorderThreshold;
    private Integer demandVelocity;
    private ProductStatus status;
    private Long version;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .currentPrice(product.getCurrentPrice())
                .costPrice(product.getCostPrice())
                .supplierId(product.getSupplierId())
                .stockLevel(product.getStockLevel())
                .reorderThreshold(product.getReorderThreshold())
                .demandVelocity(product.getDemandVelocity())
                .status(product.getStatus())
                .version(product.getVersion())
                .build();
    }
}
