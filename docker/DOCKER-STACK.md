# STP Client — Docker Stack

## Overview

Deploys `stp-client` as a **Docker Swarm** service with:
- TLS keystore injected via Docker secret
- Spring Boot config managed via Docker config (Swarm-distributed)
- Log output persisted via named volume

---

## Directory Structure

```
docker/
├── docker-stack.yml              # Swarm stack — Linux server
├── docker-stack-mac.yml          # Swarm stack — Mac paths
├── docker-stack-windows.yml      # Swarm stack — Windows paths
├── config_props/
│   ├── application.yml           # Base Spring Boot config  ← source for Docker config
│   └── application-dev.yml       # Dev profile (keystore, DB, SOAP endpoints)  ← source for Docker config
├── key/
│   └── LBBCLALABXXX.pfx          # TLS keystore (PKCS12) — source for Docker secret
└── logs/                         # Not used (logs now stored in named volume)
```

---

## Prerequisites

Before the first deploy, create the Docker secret and Docker configs.

**Linux / Mac**
```bash
# Secret — TLS keystore
docker secret create LBBCLALABXXX.pfx ./key/LBBCLALABXXX.pfx

# Configs — Spring Boot application config
docker config create stp-app-config      ./config_props/application.yml
docker config create stp-app-config-dev  ./config_props/application-dev.yml
```

**Windows**
```powershell
docker secret create LBBCLALABXXX.pfx .\key\LBBCLALABXXX.pfx

docker config create stp-app-config      .\config_props\application.yml
docker config create stp-app-config-dev  .\config_props\application-dev.yml
```

---

## Deploy

**Linux (server)**
```bash
docker stack deploy -c docker-stack.yml stp
```

**Mac**
```bash
docker stack deploy -c docker-stack-mac.yml stp
```

**Windows**
```powershell
docker stack deploy -c docker-stack-windows.yml stp
```

---

## How Docker Configs Work

Docker configs are stored in the **Swarm manager's Raft store** — not on any host filesystem.
Swarm automatically distributes them to all nodes running replicas.

```
config_props/application.yml    (host file — source only)
        │
        ▼  docker config create
Docker config: stp-app-config   (stored in Swarm Raft — encrypted)
        │
        ▼  distributed to all nodes automatically
/usr/apps/config_props/application.yml  (mounted read-only inside container)
        │
        ▼  loaded by Spring Boot via
--spring.config.location=./config_props/application.yml
```

> The host file (`config_props/application.yml`) is only used to **create or recreate** the Docker config.
> Once created, Swarm manages distribution — no manual file sync across nodes needed.

---

## How Secrets Work

```
key/LBBCLALABXXX.pfx            (host file — source only)
        │
        ▼  docker secret create
Docker secret: LBBCLALABXXX.pfx (stored encrypted in Swarm Raft)
        │
        ▼  mounted inside container at
/usr/apps/key/LBBCLALABXXX.pfx
        │
        ▼  referenced by
application-dev.yml  →  ks.path: key/LBBCLALABXXX.pfx
```

> The `target` path in the stack file must stay `/usr/apps/key/LBBCLALABXXX.pfx` to match the app config.

---

## Update application.yml or application-dev.yml

> **Must be run on the Swarm manager node.**
> Docker configs are immutable — remove and recreate to update.

**1. Edit the file on the manager host**
```bash
vi ./config_props/application.yml
```

**2. Remove old config**
```bash
docker config rm stp-app-config
```

**3. Recreate from updated file**
```bash
docker config create stp-app-config ./config_props/application.yml
```

**4. Redeploy stack** — Swarm rolling-restarts the service with the new config
```bash
docker stack deploy -c docker-stack.yml stp
```

Same steps apply for `application-dev.yml`:
```bash
docker config rm stp-app-config-dev
docker config create stp-app-config-dev ./config_props/application-dev.yml
docker stack deploy -c docker-stack.yml stp
```

---

## Update Keystore (New .pfx File)

**1. Replace the file**

_Linux / Mac_
```bash
cp /path/to/new/LBBCLALABXXX.pfx ./key/LBBCLALABXXX.pfx
```

_Windows_
```powershell
copy C:\path\to\new\LBBCLALABXXX.pfx .\key\LBBCLALABXXX.pfx
```

**2. Detach secret from running service**
```bash
docker service update --secret-rm LBBCLALABXXX.pfx stp_stp-client
```

**3. Remove old secret**
```bash
docker secret rm LBBCLALABXXX.pfx
```

**4. Create new secret**

_Linux / Mac_
```bash
docker secret create LBBCLALABXXX.pfx ./key/LBBCLALABXXX.pfx
```

_Windows_
```powershell
docker secret create LBBCLALABXXX.pfx .\key\LBBCLALABXXX.pfx
```

**5. Redeploy**
```bash
docker stack deploy -c docker-stack.yml stp
```

---

## Service Details

| Property      | Value                             |
|---------------|-----------------------------------|
| Service name  | `stp_stp-client`                  |
| Port          | `7003`                            |
| Context path  | `/stp-client`                     |
| Health check  | `GET /stp-client/actuator/health` |
| Replicas      | `1`                               |
| Placement     | Manager node only                 |
| Memory limit  | `512 MB`                          |
| CPU limit     | `1.0`                             |
| Timezone      | `Asia/Bangkok`                    |
| Spring profile| `local`                           |

---

## Monitoring

### Service Status
```bash
docker service ls
```
> `REPLICAS 1/1` = healthy &nbsp;|&nbsp; `0/1` = container is down or restarting

### Task History & Errors
```bash
docker service ps stp_stp-client
```
> Past failed tasks appear with `\_` prefix — useful for seeing crash history and error messages.

### Live Logs
```bash
# Stream all logs
docker service logs -f stp_stp-client

# Last 100 lines only
docker service logs -f --tail 100 stp_stp-client
```

### Health Check Detail
```bash
# Get container ID
docker ps

# Inspect health check results (requires jq)
docker inspect --format='{{json .State.Health}}' <container_id> | jq
```
> Shows each check result with timestamp, exit code, and actuator response.

### Auto-Refresh Status
```bash
watch -n 5 docker service ps stp_stp-client
```

### Health Check Timeline

```
t=0s     Container starts
t=0–60s  start_period — failures are ignored (app is booting)
t=30s    Check #1  (ignored if fail)
t=60s    Check #2  start_period ends
t=90s    Check #3  fail #1 counted
t=120s   Check #4  fail #2 counted
t=150s   Check #5  fail #3 counted → UNHEALTHY → Swarm restarts container
```

---

## Quick Reference

| Action                     | Command                                                          |
|----------------------------|------------------------------------------------------------------|
| Deploy stack               | `docker stack deploy -c docker-stack.yml stp`                   |
| Service status             | `docker service ls`                                              |
| Task history               | `docker service ps stp_stp-client`                               |
| Stream logs                | `docker service logs -f stp_stp-client`                          |
| List secrets               | `docker secret ls`                                               |
| List configs               | `docker config ls`                                               |
| Inspect config             | `docker config inspect stp-app-config`                           |
| Scale down                 | `docker service scale stp_stp-client=0`                          |
| Scale up                   | `docker service scale stp_stp-client=1`                          |
| Remove stack               | `docker stack rm stp`                                            |
