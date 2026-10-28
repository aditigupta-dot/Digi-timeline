package com.digitaltimeline.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Thread-safe LRU cache built on LinkedHashMap's access order. Generic in key and value. */
public final class LruCache<K, V> {
    private final Map<K, V> map;

    public LruCache(int maxEntries) {
        if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be positive");
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized V computeIfAbsent(K key, Function<K, V> loader) {
        V existing = map.get(key);
        if (existing != null) return existing;
        V created = loader.apply(key);
        if (created != null) map.put(key, created);
        return created;
    }

    public synchronized int size() {
        return map.size();
    }
}
