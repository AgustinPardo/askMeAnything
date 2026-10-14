#!/usr/bin/env bash
# Entrypoint for the "cloud / no local Ollama" image.
#
# Chat and tool calling go straight to Ollama's hosted API (https://ollama.com/v1) using
# OLLAMA_API_KEY, so no Ollama server has to run in this container or on the host.
#
#   docker run -p 7860:7860 \
#     -e OLLAMA_API_KEY=<key> \
#     -e SPRING_PROFILES_ACTIVE=prod,cloud \
#     ama-ollama-cloud:latest
#
# Notes for Render:
#   - $PORT is the only port Render is allowed to see. Meteoro binds 127.0.0.1 on purpose,
#     otherwise Render's port scanner detects 8081 and routes public traffic to the MCP server.
#   - AMA is started FIRST so the public port is bound before anything else.
set -euo pipefail

APP_HOME="${APP_HOME:-/app}"
# prod: container wiring / ports / MCP   |   cloud: chat via Ollama's hosted API
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod,cloud}"
export OLLAMA_CHAT_MODEL="${OLLAMA_CHAT_MODEL:-gemma4:31b}"
export PORT="${PORT:-7860}"
METEORO_PORT="${METEORO_PORT:-8081}"

mkdir -p /tmp/ama
log() { echo "[entrypoint] $*"; }

# --------------------------------------------------------------------------
# 0. Ollama is only needed for local inference / embeddings.
#    In the "cloud" profile it is not used at all, so we skip the check entirely
#    (on Render, resolving a host name could add ~60s of DNS retries).
# --------------------------------------------------------------------------
case ",${SPRING_PROFILES_ACTIVE}," in
    *,cloud,*)
        export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://127.0.0.1:11434}"
        log "profile: ${SPRING_PROFILES_ACTIVE} (chat via Ollama cloud API)"
        log "chat model: ${OLLAMA_CHAT_MODEL}"
        if [ -z "${OLLAMA_API_KEY:-}" ]; then
            log "WARNING: OLLAMA_API_KEY is not set - cloud chat calls will fail (401)."
            log "         Create one at https://ollama.com/settings/keys"
        fi
        log "RAG: disabled in cloud mode (the hosted API has no embeddings endpoint)."
        ;;
    *)
        export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://host.docker.internal:11434}"
        log "Ollama endpoint: ${OLLAMA_BASE_URL}"
        if curl -sf "${OLLAMA_BASE_URL}/api/tags" >/dev/null 2>&1; then
            log "Ollama is reachable."
        else
            log "WARNING: ${OLLAMA_BASE_URL} is not reachable right now."
            log "         Start Ollama on the host, or pass the right OLLAMA_BASE_URL."
        fi
        ;;
esac

# --------------------------------------------------------------------------
# 1. AMA first: it owns the public port
# --------------------------------------------------------------------------
log "starting AMA on :${PORT} (profile ${SPRING_PROFILES_ACTIVE})..."
java ${JAVA_OPTS:-} -jar "${APP_HOME}/ama.jar" --server.port="${PORT}" >/tmp/ama/ama.log 2>&1 &
AMA_PID=$!

# Wait until the public port answers before starting anything else, so Render's
# port scanner can only ever see $PORT.
for _ in $(seq 1 120); do
    if curl -sf "http://127.0.0.1:${PORT}/" >/dev/null 2>&1; then
        log "AMA is up on :${PORT}"
        break
    fi
    if ! kill -0 "${AMA_PID}" 2>/dev/null; then
        log "ERROR: AMA exited during startup"
        tail -n 50 /tmp/ama/ama.log || true
        exit 1
    fi
    sleep 2
done

# --------------------------------------------------------------------------
# 2. Meteoro MCP server - loopback only, so Render never sees port 8081
# --------------------------------------------------------------------------
log "starting Meteoro MCP server on 127.0.0.1:${METEORO_PORT} (internal)..."
java ${JAVA_OPTS_METEORO:-} -jar "${APP_HOME}/meteoro-mcp-server.jar" \
    --server.port="${METEORO_PORT}" --server.address=127.0.0.1 \
    >/tmp/ama/meteoro.log 2>&1 &
METEORO_PID=$!

cleanup() {
    log "shutting down..."
    kill "${METEORO_PID}" "${AMA_PID}" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# Keep PID 1 alive: if either process dies, stop the container so Render restarts it.
wait -n "${AMA_PID}" "${METEORO_PID}"
log "a child process exited - shutting down"
exit 1