package com.stockpulse.service;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.ProductStatus;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.dto.request.CreateProductRequest;
import com.stockpulse.dto.response.ProductResponse;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.exception.InsufficientStockException;
import com.stockpulse.exception.ProductNotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository products;
    private final PricingSuggestionRepository pricingSuggestions;
    private final ReorderSuggestionRepository reorderSuggestions;
    private final ApplicationEventPublisher events;
    @Value("${stockpulse.spike.multiplier:2.0}") private double spikeMultiplier;

    public ProductResponse createProduct(CreateProductRequest request) {
        if (products.existsById(request.getId())) throw new IllegalArgumentException("Product ID already exists");
        if (products.existsBySku(request.getSku())) throw new IllegalArgumentException("SKU already exists");
        Product product = new Product(request.getId(), request.getSku(), request.getName(), request.getCategory(),
                request.getCurrentPrice(), request.getStockLevel(), request.getReorderThreshold(), 0);
        product.setCostPrice(request.getCostPrice());
        product.setSupplierId(request.getSupplierId());
        return ProductResponse.from(products.save(product));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProducts(String status, String category) {
        ProductStatus productStatus = status == null || status.isBlank() ? null : ProductStatus.valueOf(status.toUpperCase());
        Category productCategory = category == null || category.isBlank() ? null : Category.valueOf(category.toUpperCase());
        List<Product> list;
        if (productStatus != null && productCategory != null) list = products.findByStatusAndCategory(productStatus, productCategory);
        else if (productStatus != null) list = products.findByStatus(productStatus);
        else if (productCategory != null) list = products.findByCategory(productCategory);
        else list = products.findAll();
        List<ProductResponse> items = list.stream().map(ProductResponse::from).toList();
        return Map.of("items", items, "total", items.size());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(String productId) { return ProductResponse.from(findProduct(productId)); }

    public Map<String, Object> updateStock(String productId, Integer stockLevel) {
        Product product = findProduct(productId);
        int previousStock = product.getStockLevel();
        product.setStockLevel(stockLevel);
        boolean triggered = product.isBelowThreshold();
        product.recomputeStatus(pricingSuggestions.existsByProduct_IdAndStatus(productId, SuggestionStatus.PENDING));
        products.save(product);
        if (triggered) events.publishEvent(new InventoryLowEvent(productId, TriggerReason.INVENTORY_LOW));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productId", productId);
        body.put("previousStockLevel", previousStock);
        body.put("stockLevel", stockLevel);
        body.put("status", product.getStatus());
        body.put("triggered", triggered);
        return body;
    }

    public Map<String, Object> placeOrder(String productId, Integer quantity) {
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        Product product = findProduct(productId);
        if (product.getStockLevel() < quantity) throw new InsufficientStockException(productId, quantity, product.getStockLevel());
        int previousStock = product.getStockLevel();
        product.recordSale(quantity);
        products.save(product);

        List<String> triggeredSignals = new ArrayList<>();
        if (product.isBelowThreshold()) {
            triggeredSignals.add(TriggerReason.INVENTORY_LOW.name());
            events.publishEvent(new InventoryLowEvent(productId, TriggerReason.INVENTORY_LOW));
        }
        Double peerAverage = products.averagePeerVelocity(product.getCategory(), productId);
        double average = peerAverage == null ? 0.0 : peerAverage;
        if (product.getDemandVelocity() > spikeMultiplier * average) {
            triggeredSignals.add(TriggerReason.DEMAND_SPIKE.name());
            events.publishEvent(new DemandSpikeEvent(productId, TriggerReason.DEMAND_SPIKE));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", UUID.randomUUID().toString());
        body.put("productId", productId);
        body.put("quantity", quantity);
        body.put("previousStockLevel", previousStock);
        body.put("stockLevel", product.getStockLevel());
        body.put("demandVelocity", product.getDemandVelocity());
        body.put("triggeredSignals", triggeredSignals);
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardSummary() {
        List<Product> all = products.findAll();
        long lowStock = all.stream().filter(Product::isBelowThreshold).count();
        long outOfStock = all.stream().filter(p -> p.getStockLevel() == 0).count();
        long pending = pricingSuggestions.countByStatus(SuggestionStatus.PENDING)
            + reorderSuggestions.countByStatus(SuggestionStatus.PENDING);
        long spikes = all.stream().filter(p -> {
            Double avg = products.averagePeerVelocity(p.getCategory(), p.getId());
            return p.getDemandVelocity() > spikeMultiplier * (avg == null ? 0.0 : avg);
        }).count();
        return Map.of("totalProducts", all.size(), "lowStockProducts", lowStock, "outOfStockProducts", outOfStock,
                "pendingReviews", pending, "demandSpikeProducts", spikes);
    }

    private Product findProduct(String id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
