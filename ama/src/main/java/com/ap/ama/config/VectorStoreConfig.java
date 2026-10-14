package com.ap.ama.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the (initially empty) vector store.
 *
 * <p>Two things matter for startup:
 * <ul>
 *   <li>building the store does not call the embedding model, so AMA starts even when Ollama
 *       is not running;</li>
 *   <li>the bean is only created when an {@link EmbeddingModel} exists. In the "cloud" profile
 *       embeddings are disabled (the hosted API has none), so no vector store is created and the
 *       RAG mode reports that instead of failing the whole application.</li>
 * </ul>
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}