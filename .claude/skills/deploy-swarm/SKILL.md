---
name: deploy-swarm
description: Build Docker image and deploy stp-client to Docker Swarm. Use when asked to build, deploy, redeploy, update the service, or push to registry.
allowed tools: Bash, Read
---

# Deploy STP Client to Docker Swarm

## Quick Deploy
```bash
# Build + push image then deploy
./build.sh dev

# Deploy stack (Linux)
docker stack deploy -c docker/docker-stack.yml stp

# Deploy stack (macOS dev)
docker stack deploy -c docker/docker-stack-mac.yml stp
```

## Step-by-Step

### 1. Build image
```bash
./build.sh dev [optional-tag]
# Outputs: 172.16.4.62:5000/noh/stp-client:dev-<git-sha>
```

### 2. Update stack image tag
Edit `docker/docker-stack.yml`:
```yaml
image: 172.16.4.62:5000/noh/stp-client:dev-<new-tag>
```

### 3. Deploy / update service
```bash
docker stack deploy --with-registry-auth -c docker/docker-stack.yml stp
```

### 4. Verify deployment
```bash
docker service ls | grep stp
docker service ps stp_stp-client
docker service logs stp_stp-client -f --tail 50
```

### 5. Health check
```bash
curl http://localhost:7003/stp-client/actuator/health
# Expected: {"status":"UP"}
```

## Docker Secrets & Configs
Secrets and configs are managed externally in Swarm — must exist before deploy:
```bash
# Keystore secret
docker secret create stp-keystore key/LBBCLALABXXX.pfx

# App configs
docker config create stp-app-config docker/config_props/application.yml
docker config create stp-app-config-dev docker/config_props/application-dev.yml
```

## Service Constraints
- Replicas: 1
- Placement: manager nodes only (`node.role == manager`)
- Memory limit: 512M · CPU limit: 1.0
- Update policy: stop-first, rollback on failure, 15s delay

## Rollback
```bash
docker service rollback stp_stp-client
```

## Logs
```bash
# Follow live logs
docker service logs stp_stp-client -f

# Last 100 lines
docker service logs stp_stp-client --tail 100

# Local log files (mounted volume)
ls docker/logs/
```
