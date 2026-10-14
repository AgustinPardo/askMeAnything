package com.ap.ama.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Lifecycle helper for the Meteoro MCP server.
 *
 * <p>The Spring AI MCP client autoconfiguration is configured with
 * {@code spring.ai.mcp.client.initialized=false}, so nothing is connected at startup.
 * This service connects / disconnects on demand, which lets the user start or stop the
 * external Meteoro server independently from AMA.
 */
@Service
public class MeteoroMcpService {

    private final ObjectProvider<List<McpSyncClient>> clients;

    private McpSyncClient meteoroClient;
    private boolean connected;

    public MeteoroMcpService(ObjectProvider<List<McpSyncClient>> clients) {
        this.clients = clients;
    }

    public synchronized boolean isConnected() {
        return connected && meteoroClient != null;
    }

    /** Attempts to (re)connect to the Meteoro MCP server. */
    public synchronized Map<String, Object> connect() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (isConnected()) {
            result.put("connected", true);
            result.put("tools", toolNames());
            return result;
        }
        McpSyncClient candidate = findMeteoroClient();
        if (candidate == null) {
            result.put("connected", false);
            result.put("error", "No MCP client configured for the meteoro connection. "
                    + "Check spring.ai.mcp.client.streamable-http.connections.meteoro.*");
            return result;
        }
        try {
            candidate.initialize();
            meteoroClient = candidate;
            connected = true;
            result.put("connected", true);
            result.put("tools", toolNames());
        } catch (Exception e) {
            connected = false;
            meteoroClient = null;
            result.put("connected", false);
            result.put("error", "Could not connect to the Meteoro MCP server at "
                    + "http://localhost:8081/mcp : " + rootMessage(e));
        }
        return result;
    }

    private List<String> toolNames() {
        if (meteoroClient == null) {
            return List.of();
        }
        try {
            List<String> names = new ArrayList<>();
            McpSchema.ListToolsResult tools = meteoroClient.listTools();
            if (tools != null && tools.tools() != null) {
                tools.tools().forEach(tool -> names.add(tool.name()));
            }
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }

    private McpSyncClient findMeteoroClient() {
        return clients.stream()
                .flatMap(List::stream)
                .filter(client -> client.getClientInfo() != null
                        && (client.getClientInfo().name().contains("meteoro")
                            || client.getClientInfo().title().contains("meteoro")))
                .findFirst()
                .orElseGet(() -> clients.stream().flatMap(List::stream).findFirst().orElse(null));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message != null ? message : current.getClass().getSimpleName();
    }
}