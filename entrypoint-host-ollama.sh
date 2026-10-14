#!/usr/bin/env bash
# Entrypoint for the "cloud / no local Ollama" image.
#
# Chat and tool calling go straight to Ollama's hosted API (https://ollama.com/v1) using
# OLLAMA_API_KEY, so no Ollama server has to run in this container or on the host.
#
#   docker run -p 7860:7860 \
#     -e OLLAMA_API_KEY=<key> \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     ama-ollama-cloud:latest
#
# Notes for Render:
#   - $PORT is the only port Render is allowed to see. Meteoro binds 127.0.0.1 on purpose,
#     otherwise Render's port scanner detects 8081 and routes public traffic to the MCP server.
#   - AMA is started FIRST so the public port is bound before anything else.
set -euo pipefail

APP_HOME="${APP_HOME:-/app}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export OLLAMA_CHAT_MODEL="${OLLAMA_CHAT_MODEL:-gemma4:31b}"
export PORT="${PORT:-7860}"
METEORO_PORT="${METEORO_PORT:-8081}"
LOG_DIR="${LOG_DIR:-/tmp/ama}"

mkdir -p "${LOG_DIR}"
log() { echo "[entrypoint] $*"; }

# --------------------------------------------------------------------------
# 0. Chat goes to Ollama's hosted API (profile prod), so no local Ollama is needed.
# --------------------------------------------------------------------------
log "profile: ${SPRING_PROFILES_ACTIVE} (chat via Ollama cloud API)"
log "chat model: ${OLLAMA_CHAT_MODEL}"
if [ -z "${OLLAMA_API_KEY:-}" ]; then
    log "WARNING: OLLAMA_API_KEY is not set - cloud chat calls will fail (401)."
    log "         Create one at https://ollama.com/settings/keys"
fi
log "RAG: disabled in this profile (the hosted API has no embeddings endpoint)."
log "logs: ${LOG_DIR} (also served by /api/logs/ama and /api/logs/meteoro)"

# --------------------------------------------------------------------------
# 1. AMA first: it owns the public port
# --------------------------------------------------------------------------
log "starting AMA on :${PORT}..."
java ${JAVA_OPTS:-} -jar "${APP_HOME}/ama.jar" --server.port="${PORT}" \
    --log-dir="${LOG_DIR}" >"${LOG_DIR}/ama.log" 2>&1 &
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
# 2. Meteoro MCP server - loopback only, so Render never sees port 8081.
#    'tee' makes its output show up in BOTH the Render log stream and the file.
# --------------------------------------------------------------------------
log "starting Meteoro MCP server on 127.0.0.1:${METEORO_PORT} (internal)..."
java ${JAVA_OPTS_METEORO:-} -jar "${APP_HOME}/meteoro-mcp-server.jar" \
    --server.port="${METEORO_PORT}" --server.address=127.0.0.1 \
    2>&1 | tee "${LOG_DIR}/meteoro.log" &
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