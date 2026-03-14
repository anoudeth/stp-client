package com.noh.stpclient.service;

import com.noh.stpclient.model.ServiceResult;
import com.noh.stpclient.web.dto.GetUpdatesResponseDto;
import com.noh.stpclient.web.dto.SendAckNakRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GwPollingService {

    private final GwIntegrationService gwIntegrationService;
    private final SessionManager sessionManager;

    @Scheduled(fixedDelay = 10_000)
    public void poll() {
        log.info("GwPollingService: starting poll cycle");

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

        log.info("GwPollingService: processing {} update(s)", items.size());
        for (GetUpdatesResponseDto item : items) {
            SendAckNakRequest ackRequest = new SendAckNakRequest("ACK", item.msgNetInputTime(), item.msgNetMir());
            ServiceResult<Void> ackResult = gwIntegrationService.performSendAckNak(ackRequest);
            if (ackResult.isSuccess()) {
                log.info("GwPollingService: ACK sent for MIR {}", item.msgNetMir());
            } else {
                log.warn("GwPollingService: ACK failed for MIR {} — {} {}", item.msgNetMir(), ackResult.getErrorCode(), ackResult.getErrorMessage());
            }
        }
    }
}
