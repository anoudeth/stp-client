package com.noh.stpclient.service;

import com.noh.stpclient.model.xml.GetUpdatesResponse;
import com.noh.stpclient.model.xml.GetUpdatesResponse.ParamsMtMsg;
import com.noh.stpclient.model.xml.SendResponseData;
import com.noh.stpclient.remote.GWClientMuRemote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Polls the LaPass gateway for inbound transactions using a background scheduled task.
 *
 * <p>For each poll cycle:
 * <ol>
 *   <li>Call {@code getUpdates} — returns 0..N pending inbound messages (batch)</li>
 *   <li>For each message: send ACK to the gateway first (required for RTGS settlement)</li>
 *   <li>Then forward the message to the Core Teller API for booking</li>
 * </ol>
 *
 * <p>ACK ordering: ACK is sent before Core Teller booking. If Core Teller fails after ACK,
 * that is a reconciliation issue to handle manually — the reverse (settlement without ACK)
 * is a more severe problem.
 *
 * <p>The poll interval uses {@code fixedDelay} (not {@code fixedRate}) so a slow gateway
 * response cannot cause overlapping poll cycles.
 */
@Service
@Slf4j
public class InboundPollingService {

    private final SessionManager sessionManager;
    private final GWClientMuRemote soapClient;

    @Value("${stp.polling.enabled:true}")
    private boolean pollingEnabled;

    public InboundPollingService(SessionManager sessionManager, GWClientMuRemote soapClient) {
        this.sessionManager = sessionManager;
        this.soapClient = soapClient;
    }

    @Scheduled(fixedDelayString = "${stp.polling.interval-ms:5000}",
               initialDelayString = "${stp.polling.initial-delay-ms:10000}")
    public void pollForUpdates() {
        if (!pollingEnabled) {
            return;
        }
        try {
            String sessionId = sessionManager.getSession();
            GetUpdatesResponse response = soapClient.getUpdates(sessionId);

            List<ParamsMtMsg> messages = (response == null || response.getItems() == null)
                    ? List.of() : response.getItems();

            if (messages.isEmpty()) {
                log.debug("[Polling] No inbound messages");
                return;
            }

            log.info("[Polling] Received {} inbound message(s)", messages.size());
            for (ParamsMtMsg msg : messages) {
                processInboundMessage(msg);
            }
        } catch (Exception e) {
            // Never rethrow from @Scheduled — an uncaught exception cancels all future executions
            log.error("[Polling] Poll cycle failed", e);
        }
    }

    private void processInboundMessage(ParamsMtMsg msg) {
        String mir = msg.getMsgNetMir();
        String datetime = msg.getMsgNetInputTime();
        log.info("[Polling] Processing inbound message: MIR={}, type={}, sender={}",
                mir, msg.getMsgType(), msg.getMsgSender());

        // Step 1: ACK the gateway first — do not book if ACK fails
        try {
            sendAck(mir, datetime);
        } catch (Exception e) {
            log.error("[Polling] ACK failed for MIR={} — skipping Core Teller booking", mir, e);
            return;
        }

        // Step 2: Forward to Core Teller API
        try {
            bookInCoreTeller(msg);
        } catch (Exception e) {
            // ACK was already sent — gateway will not re-deliver, so manual intervention is needed
            log.error("[Polling] Core Teller booking FAILED for MIR={}. Manual reconciliation required. block4={}",
                    mir, msg.getBlock4(), e);
        }
    }

    private void sendAck(String mir, String datetime) {
        String sessionId = sessionManager.getSession();
        SendResponseData ackData = new SendResponseData();
        ackData.setType("ACK");
        ackData.setDatetime(datetime);
        ackData.setMir(mir);
        soapClient.sendAckNak(sessionId, ackData);
        log.info("[Polling] ACK sent for MIR={}", mir);
    }

    private void bookInCoreTeller(ParamsMtMsg msg) {
        // TODO: inject CoreTellerClient and forward the inbound message for booking
        // coreTellerClient.bookInboundTransaction(msg);
        log.info("[Polling] [STUB] Core Teller booking for MIR={}, msgType={}",
                msg.getMsgNetMir(), msg.getMsgType());
    }
}
