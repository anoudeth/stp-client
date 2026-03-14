#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
#  Usage
# ─────────────────────────────────────────────
usage() {
  echo "Usage: $0 <env>"
  echo "  env: dev | uat | prod"
  exit 1
}

# ─────────────────────────────────────────────
#  Colours
# ─────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m' # No Colour

info()    { echo -e "${CYAN}==> $*${NC}"; }
success() { echo -e "${GREEN}$*${NC}"; }
error()   { echo -e "${RED}ERROR: $*${NC}" >&2; exit 1; }

# ─────────────────────────────────────────────
#  Validate argument
# ─────────────────────────────────────────────
[[ $# -lt 1 ]] && usage

ENV="$1"
REGISTRY="172.16.4.62:5000/stp-client"

case "$ENV" in
  dev)
    SHORT_HASH=$(git rev-parse --short HEAD)
    TAG="${REGISTRY}:dev-${SHORT_HASH}"
    ;;
  uat)
    VERSION=$(git describe --tags --abbrev=0 2>/dev/null) \
      || error "No git tag found. Tag a commit first: git tag v1.0.0"
    TAG="${REGISTRY}:uat-${VERSION}"
    ;;
  prod)
    VERSION=$(git describe --tags --abbrev=0 2>/dev/null) \
      || error "No git tag found. Tag a commit first: git tag v1.0.0"
    TAG="${REGISTRY}:${VERSION}"
    read -rp "Push ${TAG} to PRODUCTION? [y/N] " confirm
    [[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
    ;;
  *)
    error "Invalid environment '${ENV}'. Use dev, uat, or prod."
    ;;
esac

# ─────────────────────────────────────────────
#  Build steps
# ─────────────────────────────────────────────
info "Switch to Java 21"
source ~/.j21

info "Java & Maven versions"
java -version
mvn -v

info "Running: mvn clean compile package"
mvn clean compile package -DskipTests

info "Docker build (linux/amd64 — Apple Silicon)"
docker build --platform linux/amd64 -f Dockerfile -t "$TAG" .

info "Docker push → registry"
docker push "$TAG"

# Uncomment to copy config props to server:
# info "Copy props to server"
# scp -r src/main/resources/* admindev@10.10.20.154:/home/admindev/docker/app/bnpl_api/config_props

success "✓ Build & push completed: ${TAG}"