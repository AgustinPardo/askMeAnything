# meteoro-mcp-server

A **MCP (Model Context Protocol) server** written in Java that serves **fictional "Meteoro"
racing data**.

The point of this server is that the data is **made up**: it does not exist in any LLM's
training data, so the model can only answer questions about it by calling these tools over MCP.

It is consumed by the sibling `ama` app through Spring AI's MCP client.

## Stack

- Java 17+ (built and tested with JDK 21)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) `io.modelcontextprotocol.sdk:mcp:0.17.1`
  plus `mcp-spring-webmvc` for the HTTP transport
- Spring Boot 4.1.1 (used only to host the HTTP transport)
- Executable fat JAR: `target/meteoro-mcp-server.jar`

## Build

```bash
mvnw.cmd clean package        # Windows
./mvnw clean package          # Linux / macOS
```

> If Maven cannot delete `target/` (OneDrive file locks), remove the folder manually first.

## Two transports

### 1. Streamable HTTP (default, used by `ama`)

```bash
mvnw.cmd spring-boot:run       # or: java -jar target/meteoro-mcp-server.jar
```

Serves MCP at **`http://localhost:8081/mcp`** (change the port with `server.port`).

Because the server is an independent process you can **start and stop it at any time**.
Start it manually in its own terminal before using the *Meteoro MCP* action in `ama`;
AMA connects automatically the first time that action is used.

### 2. stdio

```bash
java -jar target/meteoro-mcp-server.jar --stdio
```

Speaks newline-delimited JSON-RPC on stdin/stdout (logs on stderr). Example:

```jsonl
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"ama","version":"1"}}}
{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"meteoro_race_times","arguments":{}}}
```

## Tools

| Tool | Arguments | Description |
|---|---|---|
| `meteoro_race_times` | – | Winning/total time per race of the 2026 season |
| `meteoro_race_info` | `raceId` (R1..R5) | Circuit, laps, weather, winner, fastest laps of one race |
| `meteoro_driver_stats` | `driverId` (optional) | Driver points, podiums, wins, fastest lap |
| `meteoro_track_list` | – | Circuits with length and corner count |
| `meteoro_season_summary` | – | Standings, total laps, fastest lap overall |

## Connecting from `ama`

`ama/src/main/resources/application.properties`:

```properties
spring.ai.mcp.client.streamable-http.connections.meteoro.url=http://localhost:8081
spring.ai.mcp.client.streamable-http.connections.meteoro.endpoint=/mcp
spring.ai.mcp.client.initialized=false
```

`initialized=false` lets `ama` start even when this server is not running; the connection is
established automatically the first time the *Meteoro MCP* action is used
(see `MeteoroMcpService`).

## Adding more fake data

Edit `MeteoroData.java` — all records are hardcoded constants, so restarting the server is enough.