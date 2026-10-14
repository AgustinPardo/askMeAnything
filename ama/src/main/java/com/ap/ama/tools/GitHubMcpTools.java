package com.ap.ama.tools;

import com.ap.ama.mcp.GitHubMcpClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class GitHubMcpTools {

    private final GitHubMcpClient client;

    public GitHubMcpTools(GitHubMcpClient client) {
        this.client = client;
    }

    @Tool(description = "Call a GitHub MCP tool by name. Pass arguments as a JSON object string. "
            + "Useful tool names: search_repositories, get_file_contents, search_code, search_issues, "
            + "search_pull_requests, list_commits, get_commit.")
    public String github(String toolName, String argumentsJson) {
        return client.callTool(toolName, argumentsJson);
    }

    @Tool(description = "Return GitHub MCP connection status")
    public String githubStatus() {
        return client.status();
    }
}
