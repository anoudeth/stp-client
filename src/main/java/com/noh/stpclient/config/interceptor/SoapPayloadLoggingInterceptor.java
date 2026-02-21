package com.noh.stpclient.config.interceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A Spring WS ClientInterceptor that logs outgoing and incoming SOAP payloads.
 * This is essential for debugging SOAP integrations and aligns with the project's logging guidelines.
 */
@Slf4j
public class SoapPayloadLoggingInterceptor implements ClientInterceptor {

    /**
     * Intercepts the request message before it's sent. Logs the full SOAP envelope.
     */
    @Override
    public boolean handleRequest(final MessageContext messageContext) throws WebServiceClientException {
        if (log.isDebugEnabled()) {
            logMessage("Sending SOAP Request", messageContext.getRequest());
        }
        return true; // Continue processing
    }

    /**
     * Intercepts the response message after it's received. Logs the full SOAP envelope.
     */
    @Override
    public boolean handleResponse(final MessageContext messageContext) throws WebServiceClientException {
        if (log.isDebugEnabled()) {
            logMessage("Received SOAP Response", messageContext.getResponse());
        }
        return true;
    }

    /**
     * Intercepts fault messages. Logs the full SOAP fault envelope.
     */
    @Override
    public boolean handleFault(final MessageContext messageContext) throws WebServiceClientException {
        if (log.isErrorEnabled()) {
            logMessage("Received SOAP Fault", messageContext.getResponse());
        }
        return true;
    }

    @Override
    public void afterCompletion(final MessageContext messageContext, final Exception ex) throws WebServiceClientException {
        // No-op, can be used for cleanup tasks.
    }

    private void logMessage(final String prefix, final org.springframework.ws.WebServiceMessage message) {
        try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            message.writeTo(buffer);
            final String payload = buffer.toString(StandardCharsets.UTF_8);

            // Using Java Text Blocks for cleaner log output
            log.debug("""
                {}
                ---
                {}
                ---""", prefix, payload);
        } catch (IOException e) {
            log.error("Failed to write SOAP message to buffer for logging", e);
        }
    }
}