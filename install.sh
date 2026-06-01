#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────
# RediCloud Installer
# Downloads, verifies, and extracts RediCloud from GitHub Releases.
# ─────────────────────────────────────────────────────────────────────

REPO="RediCloud/cloud-v2"
GITHUB_API="https://api.github.com/repos/${REPO}/releases"

# Defaults
CHANNEL="stable"
VERSION="latest"
INSTALL_DIR="."
VERIFY=true

# ─────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────

info()  { printf '[INFO]  %s\n' "$*"; }
ok()    { printf '[OK]    %s\n' "$*"; }
warn()  { printf '[WARN]  %s\n' "$*"; }
fail()  { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

usage() {
    cat <<EOF
RediCloud Installer

Usage: install.sh [OPTIONS]

Options:
  --channel <name>   Release channel: stable (default) or beta
  --version <ver>    Specific version (e.g. 2.4.0 or 2.4.0-beta.3), default: latest
  --dir <path>       Installation directory (default: current directory)
  --no-verify        Skip SHA256 checksum verification
  -h, --help         Show this help message
EOF
    exit 0
}

# ─────────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case "$1" in
        --channel)   CHANNEL="$2"; shift 2 ;;
        --version)   VERSION="$2"; shift 2 ;;
        --dir)       INSTALL_DIR="$2"; shift 2 ;;
        --no-verify) VERIFY=false; shift ;;
        -h|--help)   usage ;;
        *)           fail "Unknown option: $1. Use --help for usage." ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────
# Prerequisite checks
# ─────────────────────────────────────────────────────────────────────

check_command() {
    local cmd="$1"
    local hint="$2"
    if ! command -v "$cmd" &>/dev/null; then
        fail "$cmd is required but not found. $hint"
    fi
}

check_java_version() {
    local version_output
    version_output=$(java -version 2>&1 | head -n1)

    # Extract major version number from strings like:
    #   openjdk version "21.0.3" ...
    #   java version "1.8.0_392" ...
    local major
    major=$(echo "$version_output" | grep -oP '(?<=version ")(\d+)' | head -1)

    if [[ -z "$major" ]]; then
        fail "Could not determine Java version from: $version_output"
    fi

    if [[ "$major" -lt 21 ]]; then
        fail "Java 21+ is required (found: $major). Install from: https://adoptium.net"
    fi

    ok "java $major ($version_output)"
}

info "RediCloud Installer"
info "==================="
echo

check_command curl  "Install: apt install curl / yum install curl"
ok "curl $(curl --version | head -1 | awk '{print $2}')"

check_command unzip "Install: apt install unzip / yum install unzip"
ok "unzip found"

check_command java  "Java 21+ is required. Install from: https://adoptium.net"
check_java_version

echo

# ─────────────────────────────────────────────────────────────────────
# Resolve release from GitHub API
# ─────────────────────────────────────────────────────────────────────

resolve_release() {
    local releases_json
    releases_json=$(curl -fsSL "$GITHUB_API" 2>/dev/null) \
        || fail "Failed to fetch releases from GitHub API"

    if [[ "$VERSION" == "latest" ]]; then
        # Filter by channel: stable = non-prerelease, beta = prerelease
        local filter
        if [[ "$CHANNEL" == "stable" ]]; then
            filter='select(.prerelease == false and .draft == false)'
        elif [[ "$CHANNEL" == "beta" ]]; then
            filter='select(.prerelease == true and .draft == false)'
        else
            fail "Unknown channel: $CHANNEL. Use 'stable' or 'beta'."
        fi

        # Pick first (latest) matching release
        RELEASE_JSON=$(echo "$releases_json" | python3 -c "
import sys, json
releases = json.load(sys.stdin)
for r in releases:
    pre = r.get('prerelease', False)
    draft = r.get('draft', False)
    if draft:
        continue
    if '$CHANNEL' == 'stable' and not pre:
        json.dump(r, sys.stdout)
        sys.exit(0)
    elif '$CHANNEL' == 'beta' and pre:
        json.dump(r, sys.stdout)
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || fail "No $CHANNEL release found"
    else
        # Find specific version by tag
        local tag="v${VERSION}"
        RELEASE_JSON=$(echo "$releases_json" | python3 -c "
import sys, json
releases = json.load(sys.stdin)
for r in releases:
    if r.get('tag_name') == '${tag}':
        json.dump(r, sys.stdout)
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || fail "Release with tag $tag not found"
    fi

    TAG_NAME=$(echo "$RELEASE_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['tag_name'])")
    RELEASE_NAME=$(echo "$TAG_NAME" | sed 's/^v//')

    # Extract asset URLs
    ZIP_URL=$(echo "$RELEASE_JSON" | python3 -c "
import sys, json
assets = json.load(sys.stdin).get('assets', [])
for a in assets:
    if a['name'].endswith('.zip'):
        print(a['browser_download_url'])
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || fail "No zip asset found in release $TAG_NAME"

    CHECKSUMS_URL=$(echo "$RELEASE_JSON" | python3 -c "
import sys, json
assets = json.load(sys.stdin).get('assets', [])
for a in assets:
    if a['name'] == 'checksums.sha256':
        print(a['browser_download_url'])
        sys.exit(0)
print('')
" 2>/dev/null) || true
}

resolve_release

info "Downloading RediCloud $RELEASE_NAME ($CHANNEL)..."

# ─────────────────────────────────────────────────────────────────────
# Download
# ─────────────────────────────────────────────────────────────────────

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

ZIP_FILE="$TMPDIR/redicloud.zip"
curl -fsSL -o "$ZIP_FILE" "$ZIP_URL" \
    || fail "Failed to download $ZIP_URL"

ZIP_SIZE=$(du -h "$ZIP_FILE" | cut -f1)
ok "Downloaded redicloud ($ZIP_SIZE)"

# ─────────────────────────────────────────────────────────────────────
# Checksum verification
# ─────────────────────────────────────────────────────────────────────

if $VERIFY && [[ -n "${CHECKSUMS_URL:-}" ]]; then
    CHECKSUMS_FILE="$TMPDIR/checksums.sha256"
    if curl -fsSL -o "$CHECKSUMS_FILE" "$CHECKSUMS_URL" 2>/dev/null; then
        # Extract expected hash for the zip file
        ZIP_ASSET_NAME=$(basename "$ZIP_URL")
        EXPECTED=$(grep "$ZIP_ASSET_NAME" "$CHECKSUMS_FILE" | awk '{print $1}' || true)

        if [[ -n "$EXPECTED" ]]; then
            ACTUAL=$(sha256sum "$ZIP_FILE" | awk '{print $1}')
            if [[ "$ACTUAL" != "$EXPECTED" ]]; then
                fail "Checksum mismatch! Expected: $EXPECTED, Got: $ACTUAL"
            fi
            ok "Checksum verified"
        else
            warn "No checksum entry found for $ZIP_ASSET_NAME, skipping verification"
        fi
    else
        warn "Could not download checksums, skipping verification"
    fi
elif $VERIFY; then
    warn "No checksums available for this release, skipping verification"
fi

# ─────────────────────────────────────────────────────────────────────
# GPG signature (optional, informational only)
# ─────────────────────────────────────────────────────────────────────

if command -v gpg &>/dev/null && [[ -n "${CHECKSUMS_URL:-}" ]]; then
    SIG_URL="${CHECKSUMS_URL}.sig"
    SIG_FILE="$TMPDIR/checksums.sha256.sig"
    if curl -fsSL -o "$SIG_FILE" "$SIG_URL" 2>/dev/null; then
        if gpg --verify "$SIG_FILE" "$CHECKSUMS_FILE" 2>/dev/null; then
            ok "GPG signature verified"
        else
            warn "GPG signature verification failed (key may not be imported)"
        fi
    fi
fi

# ─────────────────────────────────────────────────────────────────────
# Extract
# ─────────────────────────────────────────────────────────────────────

mkdir -p "$INSTALL_DIR"

info "Extracting to $INSTALL_DIR..."
unzip -qo "$ZIP_FILE" -d "$INSTALL_DIR" \
    || fail "Failed to extract archive"

# Set execute permissions on start scripts
find "$INSTALL_DIR" -name '*.sh' -exec chmod +x {} + 2>/dev/null || true

ok "Installation complete"
echo
info "Next steps:"
info "  cd $INSTALL_DIR"
info "  ./start.sh"
