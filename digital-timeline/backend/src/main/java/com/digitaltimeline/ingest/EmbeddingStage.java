package com.digitaltimeline.ingest;

import com.digitaltimeline.ai.EmbeddingProvider;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class EmbeddingStage implements IngestionStage {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingStage.class);

    private final EmbeddingProvider embeddings;

    public EmbeddingStage(EmbeddingProvider embeddings) {
        this.embeddings = embeddings;
    }

    @Override
    public String name() {
        return "embedding";
    }

    @Override
    public void process(Activity a) {
        try {
            a.setEmbedding(embeddings.embed(a.searchableText()));
        } catch (AiProviderException e) {
            // Graceful degradation: the activity is still saved and can be re-embedded later via /api/reindex.
            log.warn("Embedding failed for '{}': {}", a.getTitle(), e.getMessage());
            a.setEmbedding(null);
        }
    }
}
