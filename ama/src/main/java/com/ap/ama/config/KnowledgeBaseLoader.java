package com.ap.ama.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Embeds {@link KnowledgeBase} into the vector store on first use.
 *
 * <p>Keeps application startup independent from the embedding model (Ollama): if the model is
 * unavailable the first RAG attempt fails with a clear message and is retried on the next
 * request. When no embedding model is configured at all (the "cloud" profile, where Ollama's
 * hosted API exposes no embeddings endpoint) the RAG mode is reported as unavailable.
 */
@Component
public class KnowledgeBaseLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public KnowledgeBaseLoader(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    /**
     * Ensures the knowledge base has been embedded.
     *
     * @return {@code true} when the vector store is ready for retrieval
     */
    public boolean ensureLoaded() {
        if (loaded.get()) {
            return true;
        }
        synchronized (this) {
            if (loaded.get()) {
                return true;
            }
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                log.info("No embedding model configured - the RAG mode stays unavailable.");
                return false;
            }
            try {
                List<Document> documents = KnowledgeBase.DOCUMENTS.stream()
                        .map(Document::new)
                        .toList();
                vectorStore.add(documents);
                loaded.set(true);
                log.info("Knowledge base embedded into the vector store ({} documents)", documents.size());
                return true;
            } catch (Exception e) {
                log.warn("Could not embed the knowledge base: {}", e.getMessage());
                return false;
            }
        }
    }

    /** User-facing explanation of why RAG is not available. */
    public String unavailableMessage() {
        if (vectorStoreProvider.getIfAvailable() == null) {
            return "RAG is not available in this deployment: no embedding model is configured.\n"
                    + "Ollama's hosted API does not expose an embeddings endpoint, so the "
                    + "knowledge base cannot be indexed. Run AMA with a local Ollama "
                    + "(ollama pull nomic-embed-text) to enable RAG.";
        }
        return "RAG is not ready: the knowledge base could not be embedded.\n"
                + "Start Ollama (ollama serve) and pull the embedding model: ollama pull nomic-embed-text";
    }
}