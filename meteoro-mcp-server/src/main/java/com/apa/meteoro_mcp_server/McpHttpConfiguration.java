package com.apa.meteoro_mcp_server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class McpHttpConfiguration {

    @Bean
    public WebMvcStreamableServerTransportProvider mcpTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(MeteoroTools.JSON)
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public McpSyncServer mcpSyncServer(WebMvcStreamableServerTransportProvider provider) {
        return MeteoroTools.register(
                        McpServer.sync(provider)
                                .serverInfo("meteoro-mcp-server", "0.0.1")
                                .instructions("Fictional Meteoro Grand Prix data served over streamable HTTP. "
                                        + "This data does not exist in real LLM training sets, "
                                        + "so it must be fetched through these tools."))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(
            WebMvcStreamableServerTransportProvider provider) {
        return provider.getRouterFunction();
    }
}