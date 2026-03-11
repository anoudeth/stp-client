package com.noh.stpclient.config.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A Spring WS ClientInterceptor that logs outgoing and incoming SOAP payloads.
 * Also captures raw XML into ThreadLocals so the service layer (AuditService)
 * can retrieve them on the same thread without re-serialising the message.
 */
@Slf4j
public class SoapPayloadLoggingInterceptor implements ClientInterceptor {

    // ----------------------------------------------------------------
    // ThreadLocal storage — read and cleared by AuditService.record()
    // ----------------------------------------------------------------

    private static final ThreadLocal<String> SOAP_REQUEST_HOLDER  = new ThreadLocal<>();
    private static final ThreadLocal<String> SOAP_RESPONSE_HOLDER = new ThreadLocal<>();

    /** Returns the outgoing SOAP XML captured during the last request on this thread. */
    public static String getSoapRequestXml() {
        return SOAP_REQUEST_HOLDER.get();
    }

    /** Returns the incoming SOAP XML captured during the last response on this thread. */
    public static String getSoapResponseXml() {
        return SOAP_RESPONSE_HOLDER.get();
    }

    /**
     * Clears both ThreadLocals. Must be called by AuditService after reading
     * to prevent ThreadLocal leaks in a thread-pool environment.
     */
    public static void clearSoapPayloads() {
        SOAP_REQUEST_HOLDER.remove();
        SOAP_RESPONSE_HOLDER.remove();
    }

    // ----------------------------------------------------------------
    // ClientInterceptor implementation
    // ----------------------------------------------------------------

    @Override
    public boolean handleRequest(final MessageContext messageContext) throws WebServiceClientException {
        String xml = extractPayload(messageContext.getRequest());
        SOAP_REQUEST_HOLDER.set(xml);
        if (log.isDebugEnabled()) {
            log.debug("""
                    Sending SOAP Request
                    ---
                    {}
                    ---""", xml);
        }
        return true;
    }

    @Override
    public boolean handleResponse(final MessageContext messageContext) throws WebServiceClientException {
        String xml = extractPayload(messageContext.getResponse());
        SOAP_RESPONSE_HOLDER.set(xml);
        if (log.isDebugEnabled()) {
            log.debug("""
                    Received SOAP Response
                    ---
                    {}
                    ---""", xml);
        }
        return true;
    }

    @Override
    public boolean handleFault(final MessageContext messageContext) throws WebServiceClientException {
        // Fault IS the response for error cases — capture it as the response XML
        String xml = extractPayload(messageContext.getResponse());
        SOAP_RESPONSE_HOLDER.set(xml);
        if (log.isErrorEnabled()) {
            log.error("""
                    Received SOAP Fault
                    ---
                    {}
                    ---""", xml);
        }
        return true;
    }

    @Override
    public void afterCompletion(final MessageContext messageContext, final Exception ex)
            throws WebServiceClientException {
        // No-op — ThreadLocal cleanup is responsibility of AuditService.record()
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private String extractPayload(final org.springframework.ws.WebServiceMessage message) {
        try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            message.writeTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
        } catch (IOException e) {
            log.error("Failed to extract SOAP payload for audit", e);
            return "[EXTRACTION_ERROR: " + e.getMessage() + "]";
        }
    }
}
