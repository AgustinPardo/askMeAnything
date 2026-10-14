package com.ap.ama.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Minimal MCP stdio client for the GitHub MCP server. Lifecycle is managed by {@code McpConfig}. */
public class GitHubMcpClient {

    private final String binaryPath;
    private final String githubToken;
    private final ObjectMapper mapper = new ObjectMapper();

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private boolean initialized;
    private final AtomicLong requestId = new AtomicLong(0);

    public GitHubMcpClient(String binaryPath, String githubToken) {
        this.binaryPath = binaryPath;
        this.githubToken = githubToken;
    }

    public void start() {
        try {
            if (!Files.exists(Path.of(binaryPath))) {
                return;
            }
            ProcessBuilder pb = new ProcessBuilder(binaryPath, "stdio");
            if (githubToken != null && !githubToken.isBlank()) {
                pb.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", githubToken);
                pb.environment().put("GITHUB_TOKEN", githubToken);
            }
            pb.redirectErrorStream(false);
            process = pb.start();
            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            initialized = initialize();
        } catch (Exception e) {
            initialized = false;
        }
    }

    private boolean initialize() {
        try {
            JsonNode node = request("initialize",
                    mapper.createObjectNode()
                            .put("protocolVersion", "2024-11-05")
                            .set("capabilities", mapper.createObjectNode())
                            .set("clientInfo", mapper.createObjectNode().put("name", "ama").put("version", "1.0")),
                    20);
            if (node == null || node.has("error")) {
                return false;
            }
            ObjectNode notification = mapper.createObjectNode();
            notification.put("jsonrpc", "2.0");
            notification.put("method", "notifications/initialized");
            notification.set("params", mapper.createObjectNode());
            stdin.write(mapper.writeValueAsString(notification));
            stdin.newLine();
            stdin.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void stop() {
        try {
            if (process != null) {
                process.destroyForcibly();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isConnected() {
        return initialized && process != null && process.isAlive();
    }

    public String status() {
        if (isConnected()) {
            return "GitHub MCP (stdio) ready";
        }
        return "GitHub MCP unavailable (binary missing or token required)";
    }

    public String callTool(String toolName, String argumentsJson) {
        if (!isConnected()) {
            return "GitHub MCP server is not connected. Check mcp.github.binary and GITHUB_TOKEN.";
        }
        try {
            JsonNode args;
            try {
                args = mapper.readTree(argumentsJson);
            } catch (Exception e) {
                args = mapper.createObjectNode();
            }
            if (!args.isObject()) {
                args = mapper.createObjectNode();
            }
            ObjectNode params = mapper.createObjectNode();
            params.put("name", toolName);
            params.set("arguments", args);

            JsonNode response = request("tools/call", params, 30);
            if (response == null) {
                return "No response from GitHub MCP server";
            }
            if (response.has("error")) {
                return "MCP error: " + response.get("error").toString();
            }
            JsonNode result = response.get("result");
            if (result == null) {
                return "Empty MCP result";
            }
            if (result.has("content") && result.get("content").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : result.get("content")) {
                    if (item.has("text")) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(item.get("text").asText());
                    }
                }
                String text = sb.toString();
                if (text.length() > 6000) {
                    text = text.substring(0, 6000) + "\n...[truncated]";
                }
                return text.isBlank() ? result.toString() : text;
            }
            return result.toString();
        } catch (Exception e) {
            return "GitHub MCP call failed: " + e.getMessage();
        }
    }

    private JsonNode request(String method, JsonNode params, long timeoutSeconds) {
        synchronized (this) {
            try {
                long id = requestId.incrementAndGet();
                ObjectNode request = mapper.createObjectNode();
                request.put("jsonrpc", "2.0");
                request.put("id", id);
                request.put("method", method);
                request.set("params", params);

                stdin.write(mapper.writeValueAsString(request));
                stdin.newLine();
                stdin.flush();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<JsonNode> future = executor.submit(() -> {
                    String line;
                    while ((line = stdout.readLine()) != null) {
                        JsonNode node = mapper.readTree(line);
                        if (node.has("id") && node.get("id").asLong() == id) {
                            return node;
                        }
                    }
                    return null;
                });
                try {
                    return future.get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    process.destroyForcibly();
                    initialized = false;
                    return null;
                } finally {
                    executor.shutdownNow();
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
