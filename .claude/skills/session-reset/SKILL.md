---
name: session-reset
description: Reset or diagnose the gateway session. Use when session is stuck, EW1 errors persist after retry, or you need to force a fresh logon to the STPG hub.
allowed tools: Read, Grep, Bash
---

# Session Reset Guide — STPG Hub

## Normal Session Lifecycle
```
getOrCreate() → [check AtomicReference]
    ├── session exists → return it
    └── null → acquire ReentrantLock → doLogon() → cache sessionId
                                                         ↓
                                               EW1 fault detected
                                                         ↓
                                         invalidate(sessionId) → compareAndSet → null
                                                         ↓
                                         getOrCreate() → fresh logon
```

## REST: Force Logon
```bash
POST /stp-client/gw/logon
Content-Type: application/json
{}

# Response: { "data": { "sessionId": "xxx" }, "resStatus": "SUCCESS" }
```

## REST: Force Logout (clears cached session)
```bash
POST /stp-client/gw/logout
Content-Type: application/json
{}
```

## Diagnose via Logs
```bash
# Check session events
grep "Session\|EW1\|invalidated\|logged out\|established" logs/stp-client.log | tail -30

# Check if logon is looping (repeated doLogon calls)
grep "doLogon\|Session established" logs/stp-client.log | tail -20
```

## Force Reset via Docker
```bash
# Restart the container (triggers @PreDestroy logout → clean session on next request)
docker service update --force stp_stp-client
```

## Key Classes
- `SessionManager` — `service/SessionManager.java`
  - `activeSession`: `AtomicReference<String>`
  - `loginLock`: `ReentrantLock`
  - `SESSION_EXPIRED_CODES`: `Set.of("EW1")`

## Common Issues
| Symptom | Cause | Fix |
|---|---|---|
| EW1 on every request | Session not being invalidated | Check `isSessionExpired()` is called before retry |
| Session null after valid logon | `doLogon()` returned null | Check gateway response; verify credentials |
| Concurrent logon storms | Lock not held | Read `getOrCreate()` double-checked locking |
| EW2 on fresh logon | Wrong credentials or expired cert | Verify `stp.soap.username/password` and keystore |
