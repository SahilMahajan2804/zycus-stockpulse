package com.stockpulse.domain;

import com.stockpulse.domain.enums.Direction;
import com.stockpulse.domain.enums.SuggestionSource;
import com.stockpulse.domain.enums.SuggestionStatus;
import com.stockpulse.domain.enums.TriggerReason;
import com.stockpulse.exception.InvalidSuggestionStateException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_suggestions", uniqueConstraints = @UniqueConstraint(name = "uk_pricing_dedupe", columnNames = "dedupe_key"))
@Getter @Setter @NoArgsConstructor
public class PricingSuggestion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal currentPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal recommendedPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Direction direction;
    @Column(nullable = false, precision = 4, scale = 3) private BigDecimal confidence;
    @Column(nullable = false, length = 1000) private String reasoning;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SuggestionStatus status = SuggestionStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TriggerReason triggerReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SuggestionSource source;
    @Column(name = "dedupe_key", unique = true) private String dedupeKey;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public void accept() {
        requirePending();
        product.updatePrice(recommendedPrice);
        status = SuggestionStatus.ACCEPTED;
        dedupeKey = null;
        product.recomputeStatus(false);
    }
    public void reject() {
        requirePending();
        status = SuggestionStatus.REJECTED;
        dedupeKey = null;
        product.recomputeStatus(false);
    }
    private void requirePending() {
        if (status != SuggestionStatus.PENDING) throw new InvalidSuggestionStateException("Only PENDING suggestions can transition");
    }
}
