package com.stockpulse.event;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.stockpulse.service.SuggestionRequestDispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemandSpikeListener {
    private final SuggestionRequestDispatcher dispatcher;

    @Async("advisorExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDemandSpike(DemandSpikeEvent event) {
        try { dispatcher.generate(event.productId(), event.triggerReason()); }
        catch (DataIntegrityViolationException duplicate) { log.info("Concurrent duplicate demand-spike suggestion skipped for {}", event.productId()); }
        catch (Exception ex) { log.error("Failed processing demand-spike event for {}", event.productId(), ex); }
    }
}
