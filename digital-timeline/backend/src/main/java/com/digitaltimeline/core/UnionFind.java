package com.digitaltimeline.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Disjoint-set (union-find) with path compression. Used to find "threads of work" in the graph. */
public final class UnionFind<T> {
    private final Map<T, T> parent = new HashMap<>();
    private final Map<T, Integer> rank = new HashMap<>();

    public void add(T item) {
        parent.putIfAbsent(item, item);
        rank.putIfAbsent(item, 0);
    }

    public T find(T item) {
        add(item);
        T root = item;
        while (!parent.get(root).equals(root)) root = parent.get(root);
        T cur = item;                                   // path compression
        while (!parent.get(cur).equals(root)) {
            T next = parent.get(cur);
            parent.put(cur, root);
            cur = next;
        }
        return root;
    }

    public void union(T a, T b) {
        T ra = find(a), rb = find(b);
        if (ra.equals(rb)) return;
        int rka = rank.get(ra), rkb = rank.get(rb);
        if (rka < rkb) parent.put(ra, rb);
        else if (rka > rkb) parent.put(rb, ra);
        else { parent.put(rb, ra); rank.put(ra, rka + 1); }
    }

    public Map<T, List<T>> groups() {
        Map<T, List<T>> out = new LinkedHashMap<>();
        for (T item : new ArrayList<>(parent.keySet())) {
            out.computeIfAbsent(find(item), k -> new ArrayList<>()).add(item);
        }
        return out;
    }
}
