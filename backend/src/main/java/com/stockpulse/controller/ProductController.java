package com.stockpulse.controller;

import com.stockpulse.dto.request.CreateProductRequest;
import com.stockpulse.dto.request.OrderRequest;
import com.stockpulse.dto.request.UpdateStockRequest;
import com.stockpulse.dto.response.ProductResponse;
import com.stockpulse.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.getProducts(status, category));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(productService.updateStock(productId, request.getStockLevel()));
    }

    @PostMapping("/products/{productId}/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @PathVariable String productId,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.placeOrder(productId, request.getQuantity()));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> dashboardSummary() {
        return ResponseEntity.ok(productService.dashboardSummary());
    }
}
