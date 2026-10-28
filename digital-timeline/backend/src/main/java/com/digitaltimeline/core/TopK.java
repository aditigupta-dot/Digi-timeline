package com.digitaltimeline.core;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Keeps the K best-scoring items using a bounded min-heap (PriorityQueue).
 * O(n log k) instead of sorting everything: this is what powers search ranking.
 */
public final class TopK<T> {

    public record Entry<T>(T item, double score) {}

    private final int k;
    private final PriorityQueue<Entry<T>> heap =
            new PriorityQueue<>(Comparator.comparingDouble((Entry<T> e) -> e.score()));

    public TopK(int k) {
        if (k <= 0) throw new IllegalArgumentException("k must be positive");
        this.k = k;
    }

    public void offer(T item, double score) {
        if (heap.size() < k) {
            heap.add(new Entry<>(item, score));
        } else if (score > heap.peek().score()) {
            heap.poll();
            heap.add(new Entry<>(item, score));
        }
    }

    /** Best first. */
    public List<Entry<T>> toSortedList() {
        return heap.stream()
                .sorted(Comparator.comparingDouble((Entry<T> e) -> e.score()).reversed())
                .toList();
    }

    public int size() {
        return heap.size();
    }
}
