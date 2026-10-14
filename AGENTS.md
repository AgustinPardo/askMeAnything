# AGENTS.md — working rules for this repository

## Do not run Docker builds automatically

**Never run `docker build`, `docker compose build`, `docker compose up` or `docker run`
without the user explicitly asking in the current conversation.**

Ask first, every time. Local verification with `./mvnw test` and read-only commands
(`docker images`, `docker ps`, `docker logs`) are fine without asking.

## Project layout

Single git repository at this folder (no nested repos):

```
.
├── Dockerfile.host-ollama      # the container build (Render)
├── entrypoint-host-ollama.sh   # starts AMA (public $PORT) + meteoro-mcp (127.0.0.1:8081)
├── ama/                        # Spring Boot 4.1.1 web app (the 5 modes)
└── meteoro-mcp-server/         # MCP server (Java, MCP Java SDK 0.17.1)
```

## Profiles (always one is active)

| Profile | Chat | Embeddings | How it is activated |
|---|---|---|---|
| `dev` (default) | local Ollama `localhost:11434` | local Ollama | `spring.profiles.default=dev`, or `-Dspring-boot.run.profiles=dev` |
| `prod` | Ollama cloud API `https://ollama.com/v1` | disabled | `SPRING_PROFILES_ACTIVE=prod` (set by the entrypoint) |

`application.properties` holds only settings shared by both; `application-dev.properties`
and `application-prod.properties` hold what differs. Do not duplicate a key across files.

## Deployment

Render, `Dockerfile.host-ollama`, context `.`. Env vars: `OLLAMA_API_KEY` (required),
`GITHUB_TOKEN` (optional, GitHub MCP), `OLLAMA_CHAT_MODEL` (default `gemma4:31b`),
`PORT` (set by Render). Only `$PORT` is exposed; Meteoro must stay bound to `127.0.0.1`
or Render's port scanner routes public traffic to the MCP server.

## Useful commands

```bash
# build/test (always allowed)
cd ama && mvnw.cmd test
cd meteoro-mcp-server && mvnw.cmd package

# local run
cd meteoro-mcp-server && mvnw.cmd spring-boot:run      # :8081/mcp
cd ama && mvnw.cmd spring-boot:run                       # :8080 (dev profile)
```

## Log endpoints (no auth, demo)

`GET /api/logs/ama`, `/api/logs/meteoro`, and `…/stream` for live tailing.
Files live in `/tmp/ama` inside the container and are reset on each deploy.