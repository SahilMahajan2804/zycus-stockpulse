package com.stockpulse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.enums.Category;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.dto.request.CreateProductRequest;
import com.stockpulse.dto.request.OrderRequest;
import com.stockpulse.dto.request.UpdateStockRequest;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class ProductPhase1Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Test
    void createProductSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setId("PRD-100");
        request.setSku("SKU-TEST-100");
        request.setName("Test Product");
        request.setCategory(Category.ELECTRONICS);
        request.setCurrentPrice(new BigDecimal("79.99"));
        request.setStockLevel(25);
        request.setReorderThreshold(10);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("PRD-100"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getProductsReturnsItemsAndTotal() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void updateStockSetsValueAndReturnsStatus() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setId("PRD-101");
        request.setSku("SKU-TEST-101");
        request.setName("Stock Product");
        request.setCategory(Category.HOME);
        request.setCurrentPrice(new BigDecimal("49.99"));
        request.setStockLevel(5);
        request.setReorderThreshold(2);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        UpdateStockRequest update = new UpdateStockRequest();
        update.setStockLevel(10);

        mockMvc.perform(patch("/api/products/PRD-101/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value("PRD-101"))
            .andExpect(jsonPath("$.triggered").value(false));
    }

    @Test
    void placeOrderDecreasesStockAndIncreasesDemandVelocity() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setId("PRD-102");
        request.setSku("SKU-TEST-102");
        request.setName("Order Product");
        request.setCategory(Category.APPAREL);
        request.setCurrentPrice(new BigDecimal("19.99"));
        request.setStockLevel(8);
        request.setReorderThreshold(2);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setQuantity(1);

        mockMvc.perform(post("/api/products/PRD-102/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value("PRD-102"))
            .andExpect(jsonPath("$.stockLevel").value(7))
            .andExpect(jsonPath("$.demandVelocity").value(1));
    }

    @Test
    void insufficientStockReturnsConflict() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setId("PRD-103");
        request.setSku("SKU-TEST-103");
        request.setName("Insufficient Product");
        request.setCategory(Category.ELECTRONICS);
        request.setCurrentPrice(new BigDecimal("14.99"));
        request.setStockLevel(2);
        request.setReorderThreshold(2);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setQuantity(3);

        mockMvc.perform(post("/api/products/PRD-103/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isConflict());
    }

    @Test
    void manualPricingGeneratesAiAndRuleSuggestionsAndAcceptedChoiceDeletesTheOther() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setId("PRD-110");
        request.setSku("SKU-TEST-110");
        request.setName("Dual Choice Product");
        request.setCategory(Category.ELECTRONICS);
        request.setCurrentPrice(new BigDecimal("59.99"));
        request.setStockLevel(8);
        request.setReorderThreshold(5);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products/PRD-110/suggest-pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"triggerReason\":\"MANUAL\"}"))
            .andExpect(status().isAccepted());

        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            long pending = pricingSuggestionRepository.findAll().stream()
                    .filter(s -> s.getProduct().getId().equals("PRD-110")
                        && s.getTriggerReason() == TriggerReason.MANUAL
                        && s.getStatus() == SuggestionStatus.PENDING)
                    .count();
            if (pending >= 2) break;
            Thread.sleep(50);
        }

        var pendingSuggestions = pricingSuggestionRepository.findAll().stream()
                .filter(s -> s.getProduct().getId().equals("PRD-110")
                    && s.getTriggerReason() == TriggerReason.MANUAL
                    && s.getStatus() == SuggestionStatus.PENDING)
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(2, pendingSuggestions.size());

        String selectedId = pendingSuggestions.get(0).getId().toString();
        mockMvc.perform(patch("/api/pricing-suggestions/" + selectedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());

        long remaining = pricingSuggestionRepository.findAll().stream()
                .filter(s -> s.getProduct().getId().equals("PRD-110")
                    && s.getTriggerReason() == TriggerReason.MANUAL)
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, remaining);
    }

            @Test
            void manualSuggestionIsAsyncDeduplicatedAndRequiresHumanAcceptance() throws Exception {
            CreateProductRequest request = new CreateProductRequest();
            request.setId("PRD-109");
            request.setSku("SKU-TEST-109");
            request.setName("Manual Advisor Product");
            request.setCategory(Category.HOME);
            request.setCurrentPrice(new BigDecimal("10.00"));
            request.setStockLevel(1);
            request.setReorderThreshold(5);
            mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

            String body = "{\"triggerReason\":\"MANUAL\"}";
            mockMvc.perform(post("/api/products/PRD-109/suggest-pricing")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.triggerReason").value("MANUAL"));
            mockMvc.perform(post("/api/products/PRD-109/suggest-reorder")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

            long deadline = System.nanoTime() + java.time.Duration.ofSeconds(8).toNanos();
            while (System.nanoTime() < deadline &&
                (!pricingSuggestionRepository.existsByProduct_IdAndTriggerReasonAndStatus("PRD-109", TriggerReason.MANUAL, SuggestionStatus.PENDING)
                || !reorderSuggestionRepository.existsByProduct_IdAndTriggerReasonAndStatus("PRD-109", TriggerReason.MANUAL, SuggestionStatus.PENDING))) {
                Thread.sleep(50);
            }
            org.junit.jupiter.api.Assertions.assertTrue(pricingSuggestionRepository.existsByProduct_IdAndTriggerReasonAndStatus(
                "PRD-109", TriggerReason.MANUAL, SuggestionStatus.PENDING));
            org.junit.jupiter.api.Assertions.assertTrue(reorderSuggestionRepository.existsByProduct_IdAndTriggerReasonAndStatus(
                "PRD-109", TriggerReason.MANUAL, SuggestionStatus.PENDING));

            MvcResult list = mockMvc.perform(get("/api/pricing-suggestions?productId=PRD-109&triggerReason=MANUAL"))
                .andExpect(status().isOk()).andReturn();
            var items = objectMapper.readTree(list.getResponse().getContentAsString()).path("items");
            org.junit.jupiter.api.Assertions.assertEquals(1, items.size());
            String suggestionId = items.get(0).path("id").asText();
            mockMvc.perform(get("/api/products/PRD-109")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(10.0));
            mockMvc.perform(patch("/api/pricing-suggestions/" + suggestionId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/products/PRD-109")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(11.0));
            }
}
