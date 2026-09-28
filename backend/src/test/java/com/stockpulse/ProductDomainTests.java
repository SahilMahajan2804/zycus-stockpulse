package com.stockpulse;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.exception.InvalidSuggestionStateException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProductDomainTests {
    @Test
    void saleUpdatesStockAndVelocityAndProtectsZero() {
        Product product = product(2, 0, 1);
        product.recordSale(2);
        assertEquals(0, product.getStockLevel());
        assertEquals(2, product.getDemandVelocity());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
        assertThrows(IllegalStateException.class, product::recordSale);
    }

    @Test
    void stockAndStatusRulesMatchDomainContract() {
        Product product = product(8, 3, 15);
        assertTrue(product.isBelowThreshold());
        product.addStock(2);
        assertEquals(10, product.getStockLevel());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertThrows(IllegalArgumentException.class, () -> product.addStock(0));
        product.recomputeStatus(true);
        assertEquals(ProductStatus.PRICE_REVIEW_PENDING, product.getStatus());
        product.updatePrice(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("30.00"), product.getCurrentPrice());
    }

    @Test
    void pricingSuggestionAcceptAndRejectAreOneWayTransitions() {
        Product product = product(5, 0, 2);
        PricingSuggestion accepted = pricing(product);
        accepted.accept();
        assertEquals(new BigDecimal("12.00"), product.getCurrentPrice());
        assertEquals(SuggestionStatus.ACCEPTED, accepted.getStatus());
        assertNull(accepted.getDedupeKey());
        assertThrows(InvalidSuggestionStateException.class, accepted::reject);

        PricingSuggestion rejected = pricing(product);
        rejected.setRecommendedPrice(new BigDecimal("25.00"));
        rejected.reject();
        assertEquals(new BigDecimal("12.00"), product.getCurrentPrice());
        assertEquals(SuggestionStatus.REJECTED, rejected.getStatus());
        assertThrows(InvalidSuggestionStateException.class, rejected::accept);
    }

    @Test
    void reorderAcceptanceAddsStockOnce() {
        Product product = product(5, 0, 2);
        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setProduct(product);
        suggestion.setRecommendedQuantity(4);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setDedupeKey("PRD-UNIT:MANUAL:REORDER");
        suggestion.accept();
        assertEquals(9, product.getStockLevel());
        assertNull(suggestion.getDedupeKey());
        assertThrows(InvalidSuggestionStateException.class, suggestion::accept);
    }

    private Product product(int stock, int velocity, int threshold) {
        return new Product("PRD-UNIT", "SKU-UNIT", "Unit Product", Category.ELECTRONICS,
                new BigDecimal("10.00"), stock, threshold, velocity);
    }

    private PricingSuggestion pricing(Product product) {
        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setProduct(product);
        suggestion.setCurrentPrice(product.getCurrentPrice());
        suggestion.setRecommendedPrice(new BigDecimal("12.00"));
        suggestion.setDirection(Direction.INCREASE);
        suggestion.setConfidence(new BigDecimal("0.8"));
        suggestion.setReasoning("test reason");
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(TriggerReason.MANUAL);
        suggestion.setSource(SuggestionSource.RULE_BASED);
        suggestion.setDedupeKey("PRD-UNIT:MANUAL:PRICING");
        return suggestion;
    }
}
