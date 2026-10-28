package com.digitaltimeline.ai;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.core.VectorMath;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free embeddings via the "hashing trick": unigrams, bigrams and word prefixes are hashed into a
 * fixed-size signed vector. Not as smart as a neural model, but instant, offline and deterministic,
 * so the whole demo works with zero setup. Swap in Ollama for real semantic embeddings.
 */
public class HashingEmbeddingProvider implements EmbeddingProvider {

    private final int dims;

    public HashingEmbeddingProvider(int dims) {
        this.dims = dims;
    }

    @Override
    public String name() {
        return "hashing-" + dims;
    }

    @Override
    public int dimensions() {
        return dims;
    }

    @Override
    public float[] embed(String text) {
        List<String> tokens = TextTools.tokens(text);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            counts.merge(t, 2, Integer::sum);                                            // exact word
            if (t.length() > 5) counts.merge("~" + t.substring(0, 5), 1, Integer::sum);  // crude stemming
            if (i + 1 < tokens.size()) counts.merge(t + "_" + tokens.get(i + 1), 1, Integer::sum); // bigram
        }
        float[] v = new float[dims];
        counts.forEach((term, count) -> {
            long h = fnv1a(term);
            int index = (int) ((h >>> 1) % dims);
            float sign = (h & 1L) == 0 ? 1f : -1f;
            v[index] += sign * (float) (1.0 + Math.log(count));      // sub-linear term frequency
        });
        return VectorMath.normalize(v);
    }

    private static long fnv1a(String s) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash & Long.MAX_VALUE;
    }
}
