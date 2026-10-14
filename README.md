# Ask Me Anything (AMA)

A Spring AI playground: one web page, five ways to talk to a model.

| Mode | What it does |
|---|---|
| **Free** | Plain chat with the model |
| **Tools Calling** | Spring AI `@Tool` methods (`currentTime`, `currentDay`) |
| **RAG** | Retrieves from a vector store built from the project knowledge base |
| **GitHub MCP** | Queries GitHub through the GitHub MCP server |
| **Meteoro MCP** | Queries a custom MCP server that serves fictional racing data |

## Architecture

```
                 ┌──────────────────────────── container ────────────────────────────┐
  browser  ───►  │  ama (Spring Boot, $PORT)        meteoro-mcp (127.0.0.1:8081)   │
                 │    ├─ Free / Tools ─────────►  https://ollama.com/v1  (chat)     │
                 │    ├─ GitHub MCP ──────────►  github-mcp-server (stdio child)     │
                 │    ├─ Meteoro MCP ─────────►  meteoro-mcp (/mcp, streamable HTTP)│
                 │    └─ RAG ────────────────►  local Ollama embeddings             │
                 └──────────────────────────────────────────────────────────────────┘
```

Chat and tool calling go to **Ollama's hosted API** (`https://ollama.com/v1`, OpenAI-compatible),
so no Ollama server runs in the container. The GitHub MCP server binary is downloaded during
the image build and spawned as a child process over stdio. RAG needs an *embedding* model, which
the hosted API does not provide — it therefore works locally (with `ollama serve`) and reports
"not available" in the cloud profile.

## Repository layout

```
.
├── Dockerfile.host-ollama      # build for the container (used by Render)
├── entrypoint-host-ollama.sh   # start AMA (public port) + Meteoro (loopback)
├── ama/                        # the web app
└── meteoro-mcp-server/         # the custom MCP server
```

## Run it locally

```bash
# 1. Ollama (chat + embeddings)
ollama serve
ollama pull gemma4:31b-cloud
ollama pull nomic-embed-text

# 2. Meteoro MCP server
cd meteoro-mcp-server && mvnw spring-boot:run     # http://localhost:8081/mcp

# 3. AMA
cd ama && mvnw spring-boot:run                     # http://localhost:8080
```

## Run it in Docker

```bash
docker build -f Dockerfile.host-ollama -t ama-ollama-cloud:latest .
docker run -d -p 7860:7860 \
  -e SPRING_PROFILES_ACTIVE=prod,cloud \
  -e OLLAMA_API_KEY=<key from https://ollama.com/settings/keys> \
  -e GITHUB_TOKEN=<optional GitHub PAT> \
  ama-ollama-cloud:latest
```

## Deploy to Render

| Setting | Value |
|---|---|
| Dockerfile Path | `Dockerfile.host-ollama` |
| Docker Context | `.` |
| Docker Command | *empty* |
| Pre-Deploy Command | *empty* |
| Health Check Path | `/` |

Environment variables:

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod,cloud` |
| `OLLAMA_API_KEY` | yes | <https://ollama.com/settings/keys> |
| `OLLAMA_CHAT_MODEL` | no | default `gemma4:31b` (ids from <https://ollama.com/api/tags>) |
| `GITHUB_TOKEN` | no | GitHub PAT for the GitHub MCP mode (read-only is enough) |
| `PORT` | no | set by Render |

Notes:
- Only `$PORT` is reachable from outside. Meteoro binds `127.0.0.1:8081` on purpose — otherwise
  Render's port scanner detects it and routes public traffic to the MCP server.
- The GitHub MCP server without a token falls back to an interactive device-code login, which
  cannot be completed by a public user.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `401 Unauthorized` from the model | `OLLAMA_API_KEY` missing or revoked — create a new key |
| Chat answers are empty on a local run | The cloud model needs the signed-in Ollama app; use a local model (`gemma4:31b-cloud` requires sign-in) |
| RAG says "not available" | Expected in the cloud profile — run locally with `ollama pull nomic-embed-text` |
| GitHub MCP asks for a device code | `GITHUB_TOKEN` not set |