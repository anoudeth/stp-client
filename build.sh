#!/usr/bin/env bash
# Exit immediately on error, treat unset variables as errors, fail on pipe errors
set -euo pipefail

# ─────────────────────────────────────────────
#  Usage
# ─────────────────────────────────────────────
usage() {
  echo "Usage: $0 <env> [platform]"
  echo "  env:      dev | uat | prod"
  echo "  platform: linux/amd64 | linux/arm64  (default: auto-detect from host)"
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
# Private Docker registry (internal network)
REGISTRY="172.16.4.62:5000/noh/stp-client"

# ─────────────────────────────────────────────
#  Platform
#  Auto-detects host architecture so Apple Silicon (arm64) builds native images
#  by default, avoiding emulation warnings. Can be overridden via $2.
#  uat/prod always force linux/amd64 since the registry server is x86.
# ─────────────────────────────────────────────
HOST_ARCH=$(uname -m)
DEFAULT_PLATFORM=$([ "$HOST_ARCH" = "arm64" ] && echo "linux/arm64" || echo "linux/amd64")
PLATFORM="${2:-$DEFAULT_PLATFORM}"

# uat/prod must always target the x86 registry server
if [[ "$ENV" == "uat" || "$ENV" == "prod" ]] && [[ "$PLATFORM" != "linux/amd64" ]]; then
  info "Forcing linux/amd64 for ${ENV} (registry server is x86)"
  PLATFORM="linux/amd64"
fi

# ─────────────────────────────────────────────
#  Tag strategy
#  dev  → git short hash (e.g. dev-44f6e22)   — traceability per commit
#  uat  → latest git tag  (e.g. uat-v1.2.0)   — release-based
#  prod → latest git tag  (e.g. v1.2.0)        — clean release tag
# ─────────────────────────────────────────────
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
    # Prompt for confirmation to prevent accidental prod deployments
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

# Switch to Java 21 (loads JAVA_HOME and PATH via ~/.j21 profile script)
info "Switch to Java 21"
source ~/.j21

info "Java & Maven versions"
java -version
mvn -v

# Build JAR — tests are skipped here; run them separately before deploying
info "Running: mvn clean compile package"
mvn clean compile package -DskipTests

# Build Docker image for the target platform
info "Docker build (platform=${PLATFORM})"
docker build --platform "$PLATFORM" -f Dockerfile -t "$TAG" .

# Push image to the internal registry
info "Docker push → registry"
docker push "$TAG"

success "✓ Build & push completed: ${TAG}"