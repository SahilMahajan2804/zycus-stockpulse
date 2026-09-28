package com.stockpulse.event;

import com.stockpulse.service.SuggestionService;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemandSpikeListener {
    private final SuggestionService suggestionService;

    @Async("advisorExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDemandSpike(DemandSpikeEvent event) {
        try { suggestionService.generate(event.productId(), event.triggerReason()); }
        catch (DataIntegrityViolationException duplicate) { log.info("Concurrent duplicate demand-spike suggestion skipped for {}", event.productId()); }
        catch (Exception ex) { log.error("Failed processing demand-spike event for {}", event.productId(), ex); }
    }
}
