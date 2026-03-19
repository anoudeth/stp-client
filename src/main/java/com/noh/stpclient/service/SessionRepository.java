package com.noh.stpclient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Persists the active gateway session ID to Oracle so it survives app restarts.
 * Keyed by username — one row per gateway user.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SessionRepository {

    private static final String UPSERT = """
            MERGE INTO STP_SESSION s
            USING (SELECT ? AS USERNAME, ? AS SESSION_ID, ? AS CREATED_AT FROM DUAL) src
            ON (s.USERNAME = src.USERNAME)
            WHEN MATCHED THEN
                UPDATE SET s.SESSION_ID = src.SESSION_ID, s.CREATED_AT = src.CREATED_AT
            WHEN NOT MATCHED THEN
                INSERT (USERNAME, SESSION_ID, CREATED_AT)
                VALUES (src.USERNAME, src.SESSION_ID, src.CREATED_AT)
            """;

    private static final String DELETE = "DELETE FROM STP_SESSION WHERE USERNAME = ?";
    private static final String SELECT = "SELECT SESSION_ID FROM STP_SESSION WHERE USERNAME = ?";

    private final JdbcTemplate jdbcTemplate;

    public void save(String username, String sessionId) {
        jdbcTemplate.update(UPSERT, username, sessionId, Timestamp.valueOf(LocalDateTime.now()));
        log.debug("Session persisted for user={}", username);
    }

    public void delete(String username) {
        jdbcTemplate.update(DELETE, username);
        log.debug("Session deleted for user={}", username);
    }

    public String findSessionId(String username) {
        List<String> rows = jdbcTemplate.query(SELECT,
                (rs, rowNum) -> rs.getString("SESSION_ID"), username);
        return rows.isEmpty() ? null : rows.get(0);
    }
}