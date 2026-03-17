# ─────────────────────────────────────────────
#  Usage
# ─────────────────────────────────────────────
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateSet("dev","uat","prod")]
    [string]$Env,

    [Parameter(Position=1)]
    [string]$Platform = ""
)

$REGISTRY = "172.16.4.62:5000/noh/stp-client"

# ─────────────────────────────────────────────
#  Colours
# ─────────────────────────────────────────────
function Info    { param($msg) Write-Host "==> $msg" -ForegroundColor Cyan }
function Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Err     { param($msg) Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

# ─────────────────────────────────────────────
#  Platform
#  Auto-detects host architecture. uat/prod always force linux/amd64.
# ─────────────────────────────────────────────
if (-not $Platform) {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
    $Platform = if ($arch -eq [System.Runtime.InteropServices.Architecture]::Arm64) { "linux/arm64" } else { "linux/amd64" }
}

if (($Env -eq "uat" -or $Env -eq "prod") -and $Platform -ne "linux/amd64") {
    Info "Forcing linux/amd64 for $Env (registry server is x86)"
    $Platform = "linux/amd64"
}

# ─────────────────────────────────────────────
#  Tag strategy
#  dev  → git short hash (e.g. dev-44f6e22)
#  uat  → latest git tag  (e.g. uat-v1.2.0)
#  prod → latest git tag  (e.g. v1.2.0)
# ─────────────────────────────────────────────
switch ($Env) {
    "dev" {
        $shortHash = (git rev-parse --short HEAD).Trim()
        $TAG = "${REGISTRY}:dev-${shortHash}"
    }
    "uat" {
        $version = git describe --tags --abbrev=0 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $version) { Err "No git tag found. Tag a commit first: git tag v1.0.0" }
        $TAG = "${REGISTRY}:uat-${version.Trim()}"
    }
    "prod" {
        $version = git describe --tags --abbrev=0 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $version) { Err "No git tag found. Tag a commit first: git tag v1.0.0" }
        $TAG = "${REGISTRY}:$($version.Trim())"
        $confirm = Read-Host "Push $TAG to PRODUCTION? [y/N]"
        if ($confirm -notmatch '^[Yy]$') { Write-Host "Aborted."; exit 0 }
    }
}

# ─────────────────────────────────────────────
#  Build steps
# ─────────────────────────────────────────────

# Switch to Java 21
Info "Switch to Java 21"
# Source equivalent: dot-source the j21 profile or set JAVA_HOME manually.
# Adjust the path below to match your Java 21 installation.
#$env:JAVA_HOME = "$env:USERPROFILE\.jdks\java21"
#$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
j21

Info "Java & Maven versions"
java -version
mvn -v

# Build JAR — tests are skipped here; run them separately before deploying
Info "Running: mvn clean compile package"
mvn clean compile package "-DskipTests"
if ($LASTEXITCODE -ne 0) { Err "Maven build failed" }

# Build Docker image for the target platform
Info "Docker build (platform=$Platform)"
docker build --platform $Platform -f Dockerfile -t $TAG .
if ($LASTEXITCODE -ne 0) { Err "Docker build failed" }

# Push image to the internal registry
Info "Docker push -> registry"
docker push $TAG
if ($LASTEXITCODE -ne 0) { Err "Docker push failed" }

Success "v Build & push completed: $TAG"