package com.ap.ama.config;

import com.ap.ama.mcp.GitHubMcpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    /**
     * GitHub MCP server (stdio, external binary).
     * The Meteoro MCP server is instead wired through Spring AI's MCP client
     * autoconfiguration ({@code spring.ai.mcp.client.stdio.*}).
     */
    @Bean(destroyMethod = "stop")
    public GitHubMcpClient gitHubMcpClient(
            @Value("${mcp.github.binary:/tmp/opencode/github-mcp-server}") String binaryPath,
            @Value("${github.token:}") String githubToken) {
        GitHubMcpClient client = new GitHubMcpClient(binaryPath, githubToken);
        client.start();
        return client;
    }
}