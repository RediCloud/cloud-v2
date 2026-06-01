#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────
# RediCloud Installer
# Downloads, verifies, and extracts RediCloud from GitHub Releases.
# No dependencies beyond: bash, curl, unzip, java, sha256sum
# ─────────────────────────────────────────────────────────────────────

REPO="RediCloud/cloud-v2"
GITHUB_API="https://api.github.com/repos/${REPO}/releases"

# Trusted GPG signing key (embedded, not downloaded from release)
TRUSTED_FINGERPRINT="2ABA6BC97D5FE276C1C7FCA47D569523230B5367"
TRUSTED_KEY="-----BEGIN PGP PUBLIC KEY BLOCK-----

mDMEah3hoRYJKwYBBAHaRw8BAQdAVIhWZ6k3E0yfgdd8cTGgu99Vo9syNwbvongc
oqiZ5bK0MVJlZGlDbG91ZCBSZWxlYXNlIFNpZ25pbmcgPHJlbGVhc2VAcmVkaWNs
b3VkLmRldj6IjQQTFgoANRYhBCq6a8l9X+J2wcf8pH1WlSMjC1NnBQJqHeGhAhsj
BAsJCAcEFQoJCAQWAgMBAh4FAheAAAoJEH1WlSMjC1NnpdwBAOdGlrul1r8tMNQ9
ovKCmkIchm92D/g0heaPFFoGEjcqAP9psWyhKPcitn6ci2uDX1/NzIyvq4xGtlT6
7iBWVcJCAg==
=YozT
-----END PGP PUBLIC KEY BLOCK-----"

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

# Extract a JSON string value by key from a (single-object) JSON blob.
# Usage: json_value "key" <<< "$json"
json_value() {
    sed -n 's/.*"'"$1"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1
}

# Extract asset name -> browser_download_url pairs from a release JSON.
# Outputs: <asset_name>\t<url> per line
json_assets() {
    awk -F'"' '
        /"name"[[:space:]]*:/ { name = $4 }
        /"browser_download_url"[[:space:]]*:/ { if (name) print name "\t" $4; name = "" }
    '
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

    # Extract major version from: openjdk version "21.0.3" or java version "1.8.0_392"
    local major
    major=$(echo "$version_output" | sed -n 's/.*version "\([0-9]*\).*/\1/p')

    if [[ -z "$major" ]]; then
        fail "Could not determine Java version from: $version_output"
    fi

    if [[ "$major" -lt 21 ]]; then
        fail "Java 21+ is required (found: $major). Install from: https://adoptium.net"
    fi

    ok "java $major"
}

info "RediCloud Installer"
info "==================="
echo

check_command curl     "Install: apt install curl / yum install curl"
ok "curl found"

check_command unzip    "Install: apt install unzip / yum install unzip"
ok "unzip found"

check_command sha256sum "Install: apt install coreutils"
ok "sha256sum found"

check_command java     "Java 21+ is required. Install from: https://adoptium.net"
check_java_version

echo

# ─────────────────────────────────────────────────────────────────────
# Resolve release from GitHub API
# ─────────────────────────────────────────────────────────────────────

resolve_release() {
    local release_json

    if [[ "$VERSION" != "latest" ]]; then
        # Specific version by tag
        release_json=$(curl -fsSL "$GITHUB_API/tags/v${VERSION}" 2>/dev/null) \
            || fail "Release v${VERSION} not found"
    elif [[ "$CHANNEL" == "stable" ]]; then
        # /releases/latest returns the most recent non-prerelease, non-draft
        release_json=$(curl -fsSL "$GITHUB_API/latest" 2>/dev/null) \
            || fail "No stable release found"
    elif [[ "$CHANNEL" == "beta" ]]; then
        # Find the first pre-release tag from the releases list
        local beta_tag
        beta_tag=$(curl -fsSL "$GITHUB_API?per_page=30" 2>/dev/null \
            | awk -F'"' '
                /"tag_name"[[:space:]]*:/ { tag = $4 }
                /"prerelease"[[:space:]]*:[[:space:]]*true/ { if (tag) { print tag; exit } }
            ') || true

        [[ -z "$beta_tag" ]] && fail "No beta release found"

        release_json=$(curl -fsSL "$GITHUB_API/tags/${beta_tag}" 2>/dev/null) \
            || fail "Failed to fetch beta release $beta_tag"
    else
        fail "Unknown channel: $CHANNEL. Use 'stable' or 'beta'."
    fi

    TAG_NAME=$(echo "$release_json" | json_value "tag_name")
    [[ -z "$TAG_NAME" ]] && fail "Could not parse tag_name from release"
    RELEASE_NAME="${TAG_NAME#v}"

    # Parse asset URLs
    local assets
    assets=$(echo "$release_json" | json_assets)

    ZIP_URL=$(echo "$assets" | awk -F'\t' '/\.zip\t/ { print $2; exit }')
    CHECKSUMS_URL=$(echo "$assets" | awk -F'\t' '$1 == "checksums.sha256" { print $2; exit }')
    SIGNATURE_URL=$(echo "$assets" | awk -F'\t' '$1 == "checksums.sha256.asc" { print $2; exit }')

    [[ -z "$ZIP_URL" ]] && fail "No zip asset found in release $TAG_NAME"
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

CHECKSUMS_FILE="$TMPDIR/checksums.sha256"

if $VERIFY && [[ -n "${CHECKSUMS_URL:-}" ]]; then
    if curl -fsSL -o "$CHECKSUMS_FILE" "$CHECKSUMS_URL" 2>/dev/null; then
        ZIP_ASSET_NAME=$(basename "$ZIP_URL")
        EXPECTED=$(grep "$ZIP_ASSET_NAME" "$CHECKSUMS_FILE" | awk '{print $1}' || true)

        if [[ -n "$EXPECTED" ]]; then
            ACTUAL=$(sha256sum "$ZIP_FILE" | awk '{print $1}')
            if [[ "$ACTUAL" != "$EXPECTED" ]]; then
                fail "Checksum mismatch! Expected: $EXPECTED, Got: $ACTUAL"
            fi
            ok "SHA256 checksum verified"
        else
            warn "No checksum entry for $ZIP_ASSET_NAME, skipping"
        fi
    else
        warn "Could not download checksums, skipping verification"
    fi
elif $VERIFY; then
    warn "No checksums available for this release"
fi

# ─────────────────────────────────────────────────────────────────────
# GPG signature verification (uses embedded trusted key)
# ─────────────────────────────────────────────────────────────────────

if command -v gpg &>/dev/null \
   && [[ -n "${SIGNATURE_URL:-}" ]] \
   && [[ -f "$CHECKSUMS_FILE" ]]; then

    KEY_FILE="$TMPDIR/trusted-key.asc"
    SIG_FILE="$TMPDIR/checksums.sha256.asc"

    # Write the embedded trusted key to a temp file
    echo "$TRUSTED_KEY" > "$KEY_FILE"

    if curl -fsSL -o "$SIG_FILE" "$SIGNATURE_URL" 2>/dev/null; then
        # Import the embedded trusted key into a temporary keyring
        GNUPGHOME="$TMPDIR/gnupg"
        export GNUPGHOME
        mkdir -p "$GNUPGHOME"
        chmod 700 "$GNUPGHOME"

        gpg --batch --quiet --import "$KEY_FILE" 2>/dev/null \
            || fail "Failed to import embedded signing key"

        # Verify the imported key matches our trusted fingerprint
        IMPORTED_FP=$(gpg --batch --with-colons --fingerprint 2>/dev/null \
            | awk -F: '/^fpr/{print $10; exit}')
        if [[ "$IMPORTED_FP" != "$TRUSTED_FINGERPRINT" ]]; then
            fail "Embedded key fingerprint mismatch: expected $TRUSTED_FINGERPRINT, got $IMPORTED_FP"
        fi

        # Verify the signature -- fail-closed
        if gpg --batch --verify "$SIG_FILE" "$CHECKSUMS_FILE" 2>/dev/null; then
            ok "GPG signature verified (trusted key: ${TRUSTED_FINGERPRINT:0:16}...)"
        else
            fail "GPG signature verification FAILED. The release may be tampered with."
        fi

        unset GNUPGHOME
    else
        fail "Could not download GPG signature from release"
    fi
elif ! command -v gpg &>/dev/null; then
    info "gpg not found, skipping signature verification (install gpg for added security)"
elif [[ -z "${SIGNATURE_URL:-}" ]]; then
    warn "No GPG signature available for this release"
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
