package com.digitaltimeline.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.digitaltimeline.ai.HashingEmbeddingProvider;
import org.junit.jupiter.api.Test;

class EmbeddingTest {

    @Test
    void relatedTextIsCloserThanUnrelatedText() {
        HashingEmbeddingProvider p = new HashingEmbeddingProvider(256);
        double related = VectorMath.cosine(p.embed("neo4j knowledge graph for related activities"),
                p.embed("graph database links related activities"));
        double unrelated = VectorMath.cosine(p.embed("neo4j knowledge graph for related activities"),
                p.embed("book train tickets for the holidays"));
        assertThat(related).isGreaterThan(unrelated);
    }
}
