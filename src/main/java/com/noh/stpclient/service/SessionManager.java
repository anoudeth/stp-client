package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.utils.CryptoManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a shared, long-lived LaPass gateway session.
 * Logs in once and reuses the session for all outbound sends and inbound polling.
 * Thread-safe: uses double-checked locking so only one thread re-logs in at a time.
 */
@Component
@Slf4j
public class SessionManager {

    private final GWClientMuRemote soapClient;
    private final CryptoManager cryptoManager;

    @Value("${stp.soap.username}")
    private String username;

    @Value("${stp.soap.password}")
    private String password;

    @Value("${stp.session.validity-seconds:55}")
    private long validitySeconds;

    private volatile String currentSessionId;
    private volatile Instant sessionExpiresAt = Instant.MIN;

    private final ReentrantLock lock = new ReentrantLock();

    public SessionManager(GWClientMuRemote soapClient, CryptoManager cryptoManager) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
    }

    /**
     * Returns a valid session ID, re-logging in if the current session is missing or expired.
     * Concurrent callers block on the lock; once one thread completes logon, others reuse
     * the new session without calling logon again (double-checked locking).
     */
    public String getSession() {
        if (isSessionValid()) {
            return currentSessionId; // fast path — no lock needed
        }
        lock.lock();
        try {
            if (isSessionValid()) {
                return currentSessionId; // double-check after acquiring lock
            }
            return doLogon();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Forces a new logon regardless of current session state.
     * Call this when the gateway returns a session-expired fault mid-operation.
     */
    public String forceRefresh() {
        lock.lock();
        try {
            invalidate();
            return doLogon();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Marks the current session as invalid. Does NOT call logout on the gateway.
     */
    public void invalidate() {
        currentSessionId = null;
        sessionExpiresAt = Instant.MIN;
    }

    private boolean isSessionValid() {
        return currentSessionId != null && Instant.now().isBefore(sessionExpiresAt);
    }

    private String doLogon() {
        try {
            String signedPassword = cryptoManager.signValue(password);
            LogonResponse response = soapClient.logon(username, password, signedPassword);
            if (response == null || response.getSessionId() == null) {
                throw new IllegalStateException("Logon returned null session ID");
            }
            currentSessionId = response.getSessionId();
            sessionExpiresAt = Instant.now().plusSeconds(validitySeconds);
            log.info("[SessionManager] New session obtained, valid for {}s", validitySeconds);
            return currentSessionId;
        } catch (GatewayIntegrationException e) {
            log.error("[SessionManager] Logon failed: {} - {}", e.getCode(), e.getDescription());
            throw new RuntimeException("Gateway logon failed: " + e.getDescription(), e);
        } catch (Exception e) {
            log.error("[SessionManager] Logon failed", e);
            throw new RuntimeException("Gateway logon failed: " + e.getMessage(), e);
        }
    }
}
