package com.stockpulse.domain;

import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @NotBlank
    @Column(nullable = false, unique = true, length = 32)
    private String id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String sku;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stockLevel;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer reorderThreshold;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer demandVelocity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    private String supplierId;

    @Version
    private Long version;

    public Product(String id, String sku, String name, Category category, BigDecimal currentPrice,
                   Integer stockLevel, Integer reorderThreshold, Integer demandVelocity) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity == null ? 0 : demandVelocity;
        this.status = stockLevel == null || stockLevel == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
    }

    public void recordSale() {
        if (stockLevel == null || stockLevel <= 0) {
            throw new IllegalStateException("Insufficient stock");
        }
        stockLevel--;
        demandVelocity++;
        recomputeStatus(false);
    }

    public void recordSale(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (stockLevel == null || stockLevel < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        stockLevel -= quantity;
        demandVelocity += quantity;
        recomputeStatus(false);
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stockLevel += quantity;
        recomputeStatus(false);
    }

    public void updatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        currentPrice = price;
    }

    public boolean isBelowThreshold() {
        return stockLevel != null && stockLevel < reorderThreshold;
    }

    public void recomputeStatus(boolean hasPendingPricing) {
        if (stockLevel == null || stockLevel == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (hasPendingPricing) {
            status = ProductStatus.PRICE_REVIEW_PENDING;
        } else {
            status = ProductStatus.ACTIVE;
        }
    }
}
