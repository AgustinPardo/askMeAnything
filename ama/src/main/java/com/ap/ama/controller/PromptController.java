package com.ap.ama.controller;

import com.ap.ama.PromptForm;
import com.ap.ama.config.KnowledgeBaseLoader;
import com.ap.ama.mcp.MeteoroMcpService;
import com.ap.ama.tools.GitHubMcpTools;
import com.ap.ama.tools.TimeTools;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PromptController {

    private static final List<String> ACTIONS =
            List.of("Free", "Tools Calling", "RAG", "GitHub MCP", "Meteoro MCP");

    private static final String METEORO_PREFIX = "meteoro_";

    private final ChatClient chatClient;
    private final TimeTools timeTools;
    private final GitHubMcpTools gitHubMcpTools;
    private final MeteoroMcpService meteoroMcpService;
    private final KnowledgeBaseLoader knowledgeBaseLoader;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectProvider<ToolCallbackProvider> mcpToolCallbackProviders;

    public PromptController(ChatClient.Builder chatClientBuilder, TimeTools timeTools,
                           GitHubMcpTools gitHubMcpTools,
                           MeteoroMcpService meteoroMcpService,
                           KnowledgeBaseLoader knowledgeBaseLoader,
                           ObjectProvider<VectorStore> vectorStoreProvider,
                           ObjectProvider<ToolCallbackProvider> mcpToolCallbackProviders) {
        this.chatClient = chatClientBuilder.build();
        this.timeTools = timeTools;
        this.gitHubMcpTools = gitHubMcpTools;
        this.meteoroMcpService = meteoroMcpService;
        this.knowledgeBaseLoader = knowledgeBaseLoader;
        this.vectorStoreProvider = vectorStoreProvider;
        this.mcpToolCallbackProviders = mcpToolCallbackProviders;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("promptForm", new PromptForm("", ACTIONS.get(0)));
        model.addAttribute("actions", ACTIONS);
        model.addAttribute("response", "");
        return "index";
    }

    @PostMapping("/ask")
    public String ask(@ModelAttribute PromptForm promptForm, Model model) {
        model.addAttribute("actions", ACTIONS);
        String response;
        String action = promptForm.getAction();
        String prompt = promptForm.getPrompt();
        try {
            if ("Free".equalsIgnoreCase(action)) {
                response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
            } else if ("Tools Calling".equalsIgnoreCase(action)) {
                response = chatClient.prompt()
                        .user(prompt + "\nUse the available tools when the question needs the current time or day.")
                        .tools(timeTools)
                        .call()
                        .content();
            } else if ("RAG".equalsIgnoreCase(action)) {
                response = answerWithRag(prompt);
            } else if ("GitHub MCP".equalsIgnoreCase(action)) {
                response = chatClient.prompt()
                        .user(prompt + "\nUse the github tool for GitHub repository/code/issues/PR questions.")
                        .tools(gitHubMcpTools)
                        .call()
                        .content();
            } else if ("Meteoro MCP".equalsIgnoreCase(action)) {
                response = answerWithMeteoro(prompt);
            } else {
                // 'action' comes straight from the form, so guard against unexpected values
                response = "Unknown action: " + action + ". Pick one of " + ACTIONS + ".";
            }
        } catch (Exception e) {
            response = "Error: " + e.getMessage();
        }
        model.addAttribute("response", response);
        model.addAttribute("promptForm", promptForm);
        return "index";
    }

    private String answerWithRag(String prompt) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null || !knowledgeBaseLoader.ensureLoaded()) {
            return knowledgeBaseLoader.unavailableMessage();
        }
        return chatClient.prompt()
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user(prompt)
                .call()
                .content();
    }

    private String answerWithMeteoro(String prompt) {
        if (!meteoroMcpService.isConnected()) {
            var connect = meteoroMcpService.connect();
            if (Boolean.FALSE.equals(connect.get("connected"))) {
                return "Meteoro MCP server is not available. Start it with "
                        + "'cd ../meteoro-mcp-server && mvnw.cmd spring-boot:run' "
                        + "and try again.\n\n" + connect.get("error");
            }
        }
        List<ToolCallback> callbacks = mcpToolCallbackProviders.orderedStream()
                .flatMap(provider -> List.of(provider.getToolCallbacks()).stream())
                .filter(callback -> callback.getToolDefinition().name().startsWith(METEORO_PREFIX))
                .toList();
        if (callbacks.isEmpty()) {
            return "Connected to the Meteoro MCP server, but no meteoro_* tools were found.";
        }
        return chatClient.prompt()
                .user(prompt + "\nAnswer only with data returned by the meteoro_* tools. "
                        + "This is fictional data, never invent it.")
                .toolCallbacks(callbacks)
                .call()
                .content();
    }
}