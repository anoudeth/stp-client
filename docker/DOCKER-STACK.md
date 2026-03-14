# STP Client Docker Stack

## Overview

Deploys `stp-client` as a Docker Swarm service using a keystore secret, bind-mounted config, and log volumes.

---

## Directory Structure

```
stp-client/
├── docker-stack.yml              # Swarm stack definition
├── config_props/
│   ├── application.yml           # Base Spring Boot config
│   └── application-dev.yml       # Dev profile config (keystore path, DB, SOAP)
├── key/
│   └── LBBCLALABXXX.pfx          # TLS keystore (PKCS12) — source for Docker secret
└── logs/                         # App log output (bind-mounted)
```

---

## Deploy

```bash
docker stack deploy -c docker-stack.yml stp
```

---

## How Secrets Work

```
key/LBBCLALABXXX.pfx  (host file)
        │
        ▼
Docker secret: LBBCLALABXXX.pfx   ← created with docker secret create
        │
        ▼ mounted at
/usr/apps/key/LBBCLALABXXX.pfx    (inside container)
        │
        ▼ referenced by
application-dev.yml → ks.path: key/LBBCLALABXXX.pfx
```

The `target` in docker-stack.yml must stay as `/usr/apps/key/LBBCLALABXXX.pfx` to match the app config.

---

## Update Keystore (New .pfx File)

When you receive a new `LBBCLALABXXX.pfx`, follow these steps:

**1. Replace the file**
```bash
cp /path/to/new/LBBCLALABXXX.pfx /Users/nohder/dev/docker/app/stp-client/key/LBBCLALABXXX.pfx
```

**2. Detach secret from service**
```bash
docker service update --secret-rm LBBCLALABXXX.pfx stp_stp-client
```

**3. Remove old secret**
```bash
docker secret rm LBBCLALABXXX.pfx
```

**4. Create new secret**
```bash
docker secret create LBBCLALABXXX.pfx /Users/nohder/dev/docker/app/stp-client/key/LBBCLALABXXX.pfx
```

**5. Redeploy**
```bash
docker stack deploy -c /Users/nohder/dev/docker/app/stp-client/docker-stack.yml stp
```

---

## Service Details

| Property     | Value                                      |
|--------------|--------------------------------------------|
| Service name | `stp_stp-client`                           |
| Port         | `7003`                                     |
| Context path | `/stp-client`                              |
| Health check | `GET /stp-client/actuator/health`          |
| Replicas     | 1                                          |
| Placement    | Manager node only                          |
| Memory limit | 512 MB                                     |
| CPU limit    | 1.0                                        |
| Timezone     | Asia/Bangkok                               |

---

## Useful Commands

```bash
# Check service status
docker service ls

# Check running tasks / errors
docker service ps stp_stp-client

# View logs
docker service logs -f stp_stp-client

# List secrets
docker secret ls

# Scale down
docker service scale stp_stp-client=0

# Scale up
docker service scale stp_stp-client=1
```
