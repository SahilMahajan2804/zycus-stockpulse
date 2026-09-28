package com.stockpulse.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        saveIfMissing("PRD-001", "SKU-ELEC-001", "Wireless Earbuds Pro", Category.ELECTRONICS, "79.99", 45, 20, 3);
        saveIfMissing("PRD-002", "SKU-ELEC-002", "USB-C Hub 7-Port", Category.ELECTRONICS, "34.99", 120, 30, 1);
        saveIfMissing("PRD-003", "SKU-APP-001", "Organic Cotton T-Shirt", Category.APPAREL, "24.99", 8, 15, 12);
        saveIfMissing("PRD-004", "SKU-APP-002", "Running Shorts — Navy", Category.APPAREL, "39.99", 55, 20, 2);
        saveIfMissing("PRD-005", "SKU-HOME-001", "Ceramic Pour-Over Set", Category.HOME, "49.99", 22, 10, 4);
        saveIfMissing("PRD-006", "SKU-HOME-002", "LED Desk Lamp — Dimmable", Category.HOME, "59.99", 0, 15, 0);
        saveIfMissing("PRD-007", "SKU-ELEC-003", "Portable Charger 20K", Category.ELECTRONICS, "44.99", 18, 25, 8);
        saveIfMissing("PRD-008", "SKU-APP-003", "Hoodie — Heather Grey", Category.APPAREL, "54.99", 11, 12, 15);
        Product initialReview = productRepository.findById("PRD-003").orElse(null);
        if (initialReview == null) {
            return;
        }
        initialReview.setStatus(com.stockpulse.domain.enums.ProductStatus.PRICE_REVIEW_PENDING);
        productRepository.save(initialReview);
    }

    private void saveIfMissing(String id, String sku, String name, Category category,
                               String price, int stock, int threshold, int demandVelocity) {
        if (!productRepository.existsById(id)) {
            productRepository.save(product(id, sku, name, category, price, stock, threshold, demandVelocity));
        }
    }

    private Product product(String id, String sku, String name, Category category,
                            String price, int stock, int threshold, int demandVelocity) {
        Product product = new Product(id, sku, name, category, new BigDecimal(price),
                stock, threshold, demandVelocity);
        return product;
    }
}
