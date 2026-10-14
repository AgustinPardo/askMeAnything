package com.apa.meteoro_mcp_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Meteoro MCP server.
 *
 * <p>Serves MCP over streamable HTTP at {@code http://localhost:8081/mcp}
 * (see {@link McpHttpConfiguration}). AMA connects to it as a client.
 */
@SpringBootApplication
public class MeteoroMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeteoroMcpServerApplication.class, args);
    }
}