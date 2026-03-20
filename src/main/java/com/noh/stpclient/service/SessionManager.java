package com.noh.stpclient.service;

import com.noh.stpclient.exception.GatewayIntegrationException;
import com.noh.stpclient.model.xml.LogonResponse;
import com.noh.stpclient.remote.GWClientMuRemote;
import com.noh.stpclient.utils.CryptoManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the gateway session lifecycle.
 *
 * <p>A single session is kept alive and reused across all requests. The first
 * caller that finds no active session acquires {@link #loginLock} and performs
 * logon; subsequent concurrent callers wait on the lock and then reuse the
 * session that was just established (double-checked locking pattern).
 *
 * <p>When a SOAP call fails with a session-expired fault, the caller should
 * invoke {@link #invalidate(String)} with the stale session ID and then call
 * {@link #getOrCreate()} again to get a fresh session — the compare-and-set
 * ensures only one thread triggers a re-logon.
 */
@Component
@Slf4j
public class SessionManager {

    // Gateway fault code EW1 = "Session not found or closed"
    private static final Set<String> SESSION_EXPIRED_CODES = Set.of("EW1");

    private final GWClientMuRemote soapClient;
    private final CryptoManager cryptoManager;
    private final SessionRepository sessionRepository;
    private final String username;
    private final String password;

    private final AtomicReference<String> activeSession = new AtomicReference<>();
    private final ReentrantLock loginLock = new ReentrantLock();

    public SessionManager(GWClientMuRemote soapClient,
                          CryptoManager cryptoManager,
                          SessionRepository sessionRepository,
                          @Value("${stp.soap.username}") String username,
                          @Value("${stp.soap.password}") String password) {
        this.soapClient = soapClient;
        this.cryptoManager = cryptoManager;
        this.sessionRepository = sessionRepository;
        this.username = username;
        this.password = password;
    }

    /**
     * On startup, restores any session ID persisted from the previous run.
     * If the restored session is stale, the first SOAP call will get EW1 and
     * the normal retry logic will re-establish a fresh session.
     */
    @PostConstruct
    public void restore() {
        String stored = sessionRepository.findSessionId(username);
        if (stored != null) {
            activeSession.set(stored);
            log.info("Session restored from DB: {}", stored);
        }
    }

    /**
     * Returns the active session ID, establishing a new one if none exists.
     *
     * @throws GatewayIntegrationException if the gateway rejects the logon
     * @throws RuntimeException            if logon fails for any other reason
     */
    public String getOrCreate() {
        String session = activeSession.get();
        if (session != null) return session;

        loginLock.lock();
        try {
            // Re-check after acquiring the lock — another thread may have logged in already.
            session = activeSession.get();
            if (session != null) return session;

            session = doLogon();
            activeSession.set(session);
            sessionRepository.save(username, session);
            return session;
        } finally {
            loginLock.unlock();
        }
    }

    /**
     * Clears the cached session so the next {@link #getOrCreate()} call
     * establishes a fresh one.  Uses compare-and-set so only the thread that
     * first detected the expiry clears the reference.
     */
    public void invalidate(String sessionId) {
        if (activeSession.compareAndSet(sessionId, null)) {
            sessionRepository.delete(username);
            log.warn("Session invalidated: {}", sessionId);
            try {
                soapClient.logout(sessionId);
                log.info("Gateway logout sent for invalidated session: {}", sessionId);
            } catch (Exception e) {
                log.warn("Gateway logout failed during invalidation (ignored): {}", e.getMessage());
            }
        }
    }

    /**
     * Logs out the active session and clears the cached session ID.
     * A no-op if no session is currently active.
     *
     * @throws GatewayIntegrationException propagated from the SOAP logout call
     */
    public void logout() {
        String session = activeSession.getAndSet(null);
        if (session == null) return;

        soapClient.logout(session);
        sessionRepository.delete(username);
        log.info("Session {} logged out", session);
    }

    public boolean isSessionExpired(GatewayIntegrationException e) {
        return SESSION_EXPIRED_CODES.contains(e.getCode());
    }

    @PreDestroy
    public void shutdown() {
        String session = activeSession.getAndSet(null);
        if (session != null) {
            try {
                soapClient.logout(session);
                sessionRepository.delete(username);
                log.info("Session {} closed on shutdown", session);
            } catch (Exception e) {
                log.warn("Logout on shutdown failed for session {}: {}", session, e.getMessage());
            }
        }
    }

    private String doLogon() {
        try {
            String signedPassword = cryptoManager.signValue(password);
            LogonResponse response = soapClient.logon(username, password, signedPassword);
            if (response == null || response.getSessionId() == null) {
                throw new IllegalStateException("Received empty session ID from Gateway");
            }
            String sessionId = response.getSessionId();
            log.info("Session established: {}", sessionId);
            return sessionId;
        } catch (GatewayIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gateway logon failed for user: " + username, e);
        }
    }
}