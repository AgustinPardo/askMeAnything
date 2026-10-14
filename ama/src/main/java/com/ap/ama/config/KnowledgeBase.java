package com.ap.ama.config;

import java.util.List;

/**
 * Static knowledge base used by the RAG mode: plain text facts about how this app is built.
 * The documents are embedded lazily (see {@link KnowledgeBaseLoader}) so the application can
 * start even when the embedding model (Ollama) is not running.
 */
public final class KnowledgeBase {

    public static final List<String> DOCUMENTS = List.of(
            "Project name: ama (Ask Me Anything), a Spring AI playground.",
            "Language: Java 17. Framework: Spring Boot 4.1.1 with parent org.springframework.boot:spring-boot-starter-parent.",
            "Build tool: Maven (wrapper ./mvnw, mvnw.cmd). Main build file: pom.xml.",
            "Web stack: Spring Web MVC (spring-boot-starter-webmvc) and Thymeleaf (spring-boot-starter-thymeleaf).",
            "AI stack: Spring AI 2.0.1, using the Ollama model starter (spring-ai-starter-model-ollama).",
            "Chat model: Ollama gemma4:31b-cloud, configured in application.properties as spring.ai.ollama.chat.options.model.",
            "Embedding model for RAG: Ollama nomic-embed-text (spring.ai.ollama.embedding.options.model).",
            "Vector store dependencies: spring-ai-vector-store and spring-ai-vector-store-advisor; store type is in-memory SimpleVectorStore.",
            "Full dependency list: spring-boot-starter-thymeleaf, spring-boot-starter-webmvc, spring-ai-starter-model-ollama, spring-ai-starter-model-openai, spring-ai-vector-store, spring-ai-vector-store-advisor, spring-ai-starter-mcp-client.",
            "Entry point: com.ap.ama.AmaApplication (src/main/java/com/ap/ama/AmaApplication.java) annotated @SpringBootApplication.",
            "Main package: com.ap.ama. Sub-packages: controller, tools, config, mcp.",
            "Controller: com.ap.ama.controller.PromptController handles GET / (form) and POST /ask (submit).",
            "Form bean: com.ap.ama.PromptForm with fields prompt and action.",
            "Thymeleaf template: src/main/resources/templates/index.html.",
            "UI actions/buttons: Free, Tools Calling, RAG, GitHub MCP, Meteoro MCP. Mode is sent as the 'action' form field.",
            "Free mode: sends the prompt directly to the chat model with no tools and no retrieval.",
            "Tools Calling mode: enables Spring AI tool calling using TimeTools (currentTime, currentDay) from com.ap.ama.tools.",
            "RAG mode: uses QuestionAnswerAdvisor with the SimpleVectorStore to retrieve context before answering.",
            "Tools class location: com.ap.ama.tools.TimeTools; methods currentTime() and currentDay().",
            "Config classes: com.ap.ama.config.VectorStoreConfig creates the (empty) vector store, com.ap.ama.config.KnowledgeBaseLoader embeds the documents on first RAG query.",
            "GitHub MCP mode: connects to the external GitHub MCP server binary over stdio (mcp.github.binary) and a GITHUB_TOKEN.",
            "Meteoro MCP mode: Spring AI MCP client (spring-ai-starter-mcp-client) connects to the sibling meteoro-mcp-server over streamable HTTP at http://localhost:8081/mcp.",
            "Meteoro MCP is connected on demand (spring.ai.mcp.client.initialized=false, MeteoroMcpService); the meteoro-mcp-server runs in the same container on 127.0.0.1:8081.",
            "The meteoro-mcp-server is a Java MCP server built with the MCP Java SDK; it exposes fake Meteoro racing data tools (meteoro_race_times, meteoro_race_info, meteoro_driver_stats, meteoro_track_list, meteoro_season_summary).",
            "Config file: src/main/resources with two profiles: 'dev' (local Ollama for chat and embeddings) and 'prod' (container plus the Ollama cloud API at https://ollama.com/v1).",
            "Log endpoints: GET /api/logs/ama, /api/logs/meteoro, and the same paths plus /stream for live tailing.",
            "Secrets: must not be committed; use application-local.properties or environment variables.",
            "Folder layout: src/main/java (code), src/main/resources (config + templates), src/test/java (tests), target/ (build output).",
            "Ollama server: http://localhost:11434; pull models with 'ollama pull <model>'.",
            "Build/run commands: ./mvnw spring-boot:run (run), ./mvnw test (tests), ./mvnw -q package (package).",
            "Wrapper files: mvnw, mvnw.cmd, .mvn/ directory."
    );

    private KnowledgeBase() {
    }
}