package com.noh.stpclient.service;

import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.web.dto.GetUpdatesResponseDto;
import com.noh.stpclient.web.dto.SendAckNakRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class GwPollingService {

    private static final int POLL_DELAY_SECONDS = 55;

    private final GwIntegrationService gwIntegrationService;
    private final SessionManager sessionManager;

    // Prevents overlapping executions if a poll cycle takes longer than the scheduled interval
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Increments each cycle — used as a correlation ID in logs
    private final AtomicLong cycleCount = new AtomicLong(0);

    // Tracks when the next poll is scheduled to start (set after each poll completes)
    private volatile Instant nextPollAt = null;

    // Runs every 55 seconds after the previous execution completes
    @Scheduled(fixedDelay = POLL_DELAY_SECONDS * 1000)
    public void poll() {
        // Skip if previous poll is still in progress
        if (!running.compareAndSet(false, true)) {
            log.warn("[POLL] skipped — previous cycle still running");
            return;
        }

        nextPollAt = null; // clear countdown — poll is now confirmed to start

        long cycle = cycleCount.incrementAndGet();
        String tag = "[POLL#" + cycle + "]";
        Instant pollStart = Instant.now();

        try {
            log.info("{} ── START ────────────────────────────────", tag);

            // Reuse existing session or create a new one
            String sessionId = sessionManager.getOrCreate();
            ServiceResult<List<GetUpdatesResponseDto>> result = gwIntegrationService.performGetUpdates(sessionId);

            if (!result.isSuccess()) {
                log.warn("{} getUpdates FAILED | code={} msg={}", tag, result.getErrorCode(), result.getErrorMessage());
                return;
            }

            List<GetUpdatesResponseDto> items = result.getData();
            if (items == null || items.isEmpty()) {
                log.info("{} getUpdates OK | no new messages", tag);
                return;
            }

            log.info("{} getUpdates OK | {} message(s) received", tag, items.size());

            // Send ACK for each received message
            for (GetUpdatesResponseDto item : items) {
                Instant ackStart = Instant.now();
                SendAckNakRequest ackRequest = new SendAckNakRequest("ACK", item.msgNetInputTime(), item.msgNetMir());
                ServiceResult<Void> ackResult = gwIntegrationService.performSendAckNak(ackRequest);
                long ackElapsed = Instant.now().getEpochSecond() - ackStart.getEpochSecond();

                if (ackResult.isSuccess()) {
                    log.info("{} sendAckNak OK  | MIR={} | {}s", tag, item.msgNetMir(), ackElapsed);
                } else {
                    log.warn("{} sendAckNak FAIL| MIR={} | code={} msg={} | {}s",
                            tag, item.msgNetMir(), ackResult.getErrorCode(), ackResult.getErrorMessage(), ackElapsed);
                }
            }

        } finally {
            long totalElapsed = Instant.now().getEpochSecond() - pollStart.getEpochSecond();
            log.info("{} ── END | elapsed={}s ──────────────────────", tag, totalElapsed);
            // Always release the lock so the next cycle can run
            running.set(false);
            // Set reference time so countdown() knows when next poll is expected
            nextPollAt = Instant.now().plusSeconds(POLL_DELAY_SECONDS);
        }
    }

    // Ticks every 5 seconds — logged at DEBUG to avoid cluttering INFO logs
    @Scheduled(fixedRate = 5_000)
    public void countdown() {
        Instant next = nextPollAt;
        if (next == null) return; // poll is running or app just started

        long secondsLeft = next.getEpochSecond() - Instant.now().getEpochSecond();
        if (secondsLeft > 0) {
            // Round up to nearest 5 to avoid display drift from ticker offset
            long display = (long) (Math.ceil(secondsLeft / 5.0) * 5);
            log.debug("[COUNTDOWN] next poll in {}s", display);
        }
    }
}
