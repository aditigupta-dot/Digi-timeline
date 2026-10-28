package com.digitaltimeline.ai;

public interface EmbeddingProvider {

    String name();

    int dimensions();

    /** Returns an L2-normalised vector, so cosine similarity is just a dot product. */
    float[] embed(String text);
}
