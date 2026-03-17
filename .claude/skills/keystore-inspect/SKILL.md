---
name: keystore-inspect
description: Inspect, import, or troubleshoot the PKCS12 keystore used for CMS signing (logon) and XAdES signing (send). Use when certificate issues arise, verifying key aliases, or importing gateway certificates.
allowed tools: Bash
---

# Keystore Management — STP Client

## Keystore Details
- **Format:** PKCS12
- **Path (local):** `key/LBBCLALABXXX.pfx`
- **Path (Docker):** `/usr/apps/key/LBBCLALABXXX.pfx`
- **Alias (signing key):** `te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971`
- **Alias (gateway cert):** `gw-cert`

## List All Entries
```bash
keytool -list -v \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12
```

## Check Specific Alias
```bash
keytool -list -v \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12 \
  -alias gw-cert
```

## Import Gateway Certificate (PEM)
```bash
keytool -importcert \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12 \
  -alias gw-cert \
  -file gateway.cer \
  -noprompt
```

## Import Gateway Certificate (DER)
```bash
keytool -importcert \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12 \
  -alias gw-cert \
  -file gateway.der \
  -noprompt
```

## Export Certificate from Keystore
```bash
keytool -exportcert \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12 \
  -alias te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971 \
  -file my-cert.cer \
  -rfc
```

## Check Certificate Expiry
```bash
keytool -list -v \
  -keystore key/LBBCLALABXXX.pfx \
  -storetype PKCS12 \
  -alias te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971 \
  | grep -E "Valid|Alias|Owner|Issuer"
```

## Config Reference
```yaml
# application-dev.yml
ks:
  type: PKCS12
  path: key/LBBCLALABXXX.pfx
  alias: te-c44b72d1-77d0-4664-bb5c-a61eaa6fe971

gw.cert.alias: gw-cert
```

## CryptoManager Usage
- **`signValue(password)`** — CMS sign with private key (alias from `ks.alias`)
- **`signXml(xmlString)`** — XAdES sign DataPDU XML
- **`verifyResponseSignature(data)`** — Verify gateway CMS using cert at `gw.cert.alias`

## Docker Secret Update
```bash
# Remove old secret
docker secret rm stp-keystore

# Create new secret
docker secret create stp-keystore key/LBBCLALABXXX.pfx

# Redeploy service to pick up new secret
docker service update --force stp_stp-client
```
