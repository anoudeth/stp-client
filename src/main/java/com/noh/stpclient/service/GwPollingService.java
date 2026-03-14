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

@Service
@Slf4j
@RequiredArgsConstructor
public class GwPollingService {

    private static final int POLL_DELAY_SECONDS = 10;

    private final GwIntegrationService gwIntegrationService;
    private final SessionManager sessionManager;

    // Prevents overlapping executions if a poll cycle takes longer than the scheduled interval
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Tracks when the next poll is scheduled to start (set after each poll completes)
    private volatile Instant nextPollAt = null;

    // Runs every 10 seconds after the previous execution completes
    @Scheduled(fixedDelay = POLL_DELAY_SECONDS * 1000)
    public void poll() {
        nextPollAt = null; // clear countdown — poll is starting now

        // Skip if previous poll is still in progress
        if (!running.compareAndSet(false, true)) {
            log.warn("GwPollingService: previous poll still running — skipping");
            return;
        }
        Instant pollStart = Instant.now();
        try {
            log.info("GwPollingService: starting poll cycle");

            // Reuse existing session or create a new one
            String sessionId = sessionManager.getOrCreate();
            ServiceResult<List<GetUpdatesResponseDto>> result = gwIntegrationService.performGetUpdates(sessionId);

            if (!result.isSuccess()) {
                log.warn("GwPollingService: GetUpdates failed — {} {}", result.getErrorCode(), result.getErrorMessage());
                return;
            }

            List<GetUpdatesResponseDto> items = result.getData();
            if (items == null || items.isEmpty()) {
                log.info("GwPollingService: no updates");
                return;
            }

            // Send ACK for each received message
            log.info("GwPollingService: processing {} update(s)", items.size());
            for (GetUpdatesResponseDto item : items) {
                Instant ackStart = Instant.now();
                SendAckNakRequest ackRequest = new SendAckNakRequest("ACK", item.msgNetInputTime(), item.msgNetMir());
                ServiceResult<Void> ackResult = gwIntegrationService.performSendAckNak(ackRequest);
                long ackElapsed = Instant.now().getEpochSecond() - ackStart.getEpochSecond();
                if (ackResult.isSuccess()) {
                    log.info("GwPollingService: ACK sent for MIR {} ({}s)", item.msgNetMir(), ackElapsed);
                } else {
                    log.warn("GwPollingService: ACK failed for MIR {} — {} {} ({}s)", item.msgNetMir(), ackResult.getErrorCode(), ackResult.getErrorMessage(), ackElapsed);
                }
            }
        } finally {
            long totalElapsed = Instant.now().getEpochSecond() - pollStart.getEpochSecond();
            log.info("GwPollingService: poll cycle completed in {}s", totalElapsed);
            // Always release the lock so the next cycle can run
            running.set(false);
            // Schedule countdown: next poll fires after POLL_DELAY_SECONDS from now
            nextPollAt = Instant.now().plusSeconds(POLL_DELAY_SECONDS);
        }
    }

    // Ticks every second to log countdown until the next poll
    @Scheduled(fixedRate = 1_000)
    public void countdown() {
        Instant next = nextPollAt;
        if (next == null) return; // poll is running or app just started

        long secondsLeft = next.getEpochSecond() - Instant.now().getEpochSecond();
        if (secondsLeft > 0) {
            log.info("GwPollingService: next poll in {}s", secondsLeft);
        }
    }
}
