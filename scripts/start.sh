#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────
# RediCloud Node Starter
# ─────────────────────────────────────────────────────────────────────

JAR="redicloud-node-service-%version%.jar"
SCREEN_NAME="redicloud"

# Defaults
USE_SCREEN=false
DEBUG=false
DEBUG_PORT=5005
VERBOSE=false
NO_PING=false
EXTRA_ARGS=()

# ─────────────────────────────────────────────────────────────────────
# Usage
# ─────────────────────────────────────────────────────────────────────

usage() {
    cat <<EOF
RediCloud Node Starter

Usage: start.sh [OPTIONS]

Options:
  --screen             Run inside a GNU screen session
  --debug              Enable JDWP remote debugger (default port: 5005)
  --port <port>        Set debugger port (implies --debug)
  --verbose            Set log level to FINEST
  --no-ping            Disable node-ping task (useful for local development)
  -h, --help           Show this help message

Examples:
  ./start.sh                        # normal start
  ./start.sh --screen               # run in screen session
  ./start.sh --debug                # debugger on port 5005
  ./start.sh --debug --port 9999    # debugger on custom port
  ./start.sh --screen --verbose     # screen + verbose logging
EOF
    exit 0
}

# ─────────────────────────────────────────────────────────────────────
# Argument parsing
# ─────────────────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case "$1" in
        --screen)    USE_SCREEN=true; shift ;;
        --debug)     DEBUG=true; shift ;;
        --port)      DEBUG=true; DEBUG_PORT="$2"; shift 2 ;;
        --verbose)   VERBOSE=true; shift ;;
        --no-ping)   NO_PING=true; shift ;;
        -h|--help)   usage ;;
        *)           EXTRA_ARGS+=("$1"); shift ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────
# Build JVM arguments
# ─────────────────────────────────────────────────────────────────────

JVM_ARGS=(
    # Redisson/Gson/Guice/Kotlin Reflect: deep reflection on core classes
    --add-opens=java.base/java.lang=ALL-UNNAMED
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
    # Redisson: concurrent data structure serialization
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
    # Gson: date/time serializers
    --add-opens=java.base/java.text=ALL-UNNAMED
    # Redisson/Gson: collection serialization
    --add-opens=java.base/java.util=ALL-UNNAMED
    # Redisson/Gson: BigDecimal/BigInteger serialization
    --add-opens=java.base/java.math=ALL-UNNAMED
    # Netty (via Redisson): Unsafe for off-heap memory
    --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED
    # Netty (via Redisson): DirectByteBuffer + NIO selector optimization
    --add-opens=java.base/java.nio=ALL-UNNAMED
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
    # Ktor CIO / custom ClassLoader: socket + URL internals
    --add-opens=java.base/java.net=ALL-UNNAMED
    --add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED
)

if $DEBUG; then
    JVM_ARGS+=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}")
fi

if $VERBOSE; then
    JVM_ARGS+=("-Dredicloud.logging.level=FINEST")
fi

if $NO_PING; then
    JVM_ARGS+=("-Dredicloud.task.node-ping.disable=true")
fi

JVM_ARGS+=("-jar" "$JAR")
JVM_ARGS+=("${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}")

# ─────────────────────────────────────────────────────────────────────
# Launch
# ─────────────────────────────────────────────────────────────────────

if $USE_SCREEN; then
    if ! command -v screen &>/dev/null; then
        echo "[ERROR] screen is not installed. Install: apt install screen" >&2
        exit 1
    fi
    exec screen -S "$SCREEN_NAME" java "${JVM_ARGS[@]}"
else
    exec java "${JVM_ARGS[@]}"
fi
