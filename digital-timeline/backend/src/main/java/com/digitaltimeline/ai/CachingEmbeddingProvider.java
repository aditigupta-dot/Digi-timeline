package com.digitaltimeline.ai;

import com.digitaltimeline.core.LruCache;

/** DECORATOR pattern: wraps any EmbeddingProvider and adds an LRU cache without changing the interface. */
public class CachingEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProvider delegate;
    private final LruCache<String, float[]> cache;

    public CachingEmbeddingProvider(EmbeddingProvider delegate, int capacity) {
        this.delegate = delegate;
        this.cache = new LruCache<>(capacity);
    }

    @Override
    public String name() {
        return delegate.name() + "+cache";
    }

    @Override
    public int dimensions() {
        return delegate.dimensions();
    }

    @Override
    public float[] embed(String text) {
        return cache.computeIfAbsent(text, delegate::embed);
    }
}
