package com.digitaltimeline.service;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.core.TopK;
import com.digitaltimeline.core.VectorMath;
import com.digitaltimeline.domain.Activity;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Component;

/**
 * In-memory HYBRID index: every activity is stored as (embedding vector + keyword token set).
 * Search = 70% semantic cosine similarity + 30% keyword coverage, ranked with a bounded min-heap (TopK).
 * Many readers can search at once (read lock); writers (ingest / delete) take the write lock.
 * For millions of rows this is where pgvector / HNSW would take over (see docs/ARCHITECTURE.md).
 */
@Component
public class SearchIndex {

    public record Hit(UUID id, double score, double semantic, double keyword) {}

    private record Entry(UUID id, UUID userId, Instant occurredAt, float[] vector, Set<String> tokens) {}

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void put(Activity a) {
        if (a.getEmbedding() == null) return;
        Entry e = new Entry(a.getId(), a.getUserId(), a.getOccurredAt(), a.getEmbedding(),
                TextTools.tokenSet(a.searchableText()));
        lock.writeLock().lock();
        try {
            entries.put(a.getId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(UUID id) {
        lock.writeLock().lock();
        try {
            entries.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** from/to may be null (no time filter). minScore drops weak matches. */
    public List<Hit> search(UUID userId, float[] queryVector, Set<String> queryTokens, int k,
                            Instant from, Instant to, double minScore) {
        TopK<Hit> top = new TopK<>(k);
        lock.readLock().lock();
        try {
            for (Entry e : entries.values()) {
                if (!e.userId().equals(userId)) continue;
                if (from != null && e.occurredAt().isBefore(from)) continue;
                if (to != null && !e.occurredAt().isBefore(to)) continue;
                double semantic = e.vector().length == queryVector.length
                        ? Math.max(0, VectorMath.cosine(e.vector(), queryVector)) : 0;
                double keyword = TextTools.coverage(queryTokens, e.tokens());
                double score = 0.7 * semantic + 0.3 * keyword;
                if (score >= minScore) top.offer(new Hit(e.id(), score, semantic, keyword), score);
            }
        } finally {
            lock.readLock().unlock();
        }
        return top.toSortedList().stream().map(TopK.Entry::item).toList();
    }

    /** Pure semantic neighbours of one vector (used by the auto-linker). */
    public List<Hit> similarTo(Activity a, int k, double minSemantic) {
        TopK<Hit> top = new TopK<>(k + 1);
        lock.readLock().lock();
        try {
            for (Entry e : entries.values()) {
                if (!e.userId().equals(a.getUserId()) || e.id().equals(a.getId())) continue;
                if (e.vector().length != a.getEmbedding().length) continue;
                double s = VectorMath.cosine(e.vector(), a.getEmbedding());
                if (s >= minSemantic) top.offer(new Hit(e.id(), s, s, 0), s);
            }
        } finally {
            lock.readLock().unlock();
        }
        return top.toSortedList().stream().map(TopK.Entry::item).limit(k).toList();
    }
}
