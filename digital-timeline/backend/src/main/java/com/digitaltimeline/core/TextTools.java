package com.digitaltimeline.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tokenising, keyword extraction and set similarity. Pure functions, easy to test. */
public final class TextTools {
    private TextTools() {}

    private static final Set<String> STOP_WORDS = Set.copyOf(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "if", "then", "else", "for", "of", "to", "in", "on", "at", "by",
            "with", "from", "as", "is", "are", "was", "were", "be", "been", "it", "its", "this", "that", "these",
            "those", "i", "me", "my", "we", "our", "you", "your", "he", "she", "they", "them", "do", "did", "does",
            "done", "have", "has", "had", "what", "which", "who", "when", "where", "why", "how", "about", "into",
            "over", "after", "before", "up", "down", "out", "all", "any", "some", "no", "not", "so", "can", "will",
            "just", "than", "too", "very", "show", "tell", "give", "last", "week", "month", "today", "yesterday",
            "ago", "week", "working", "work", "worked"));

    private static final Pattern HASHTAG = Pattern.compile("(?<![\\w&])#([\\p{L}][\\p{L}\\p{N}_-]{1,30})");

    public static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() > 1 && !STOP_WORDS.contains(t))
                .toList();
    }

    public static Set<String> tokenSet(String text) {
        return new HashSet<>(tokens(text));
    }

    /** Jaccard similarity of two sets: |A ∩ B| / |A ∪ B|. */
    public static <T> double jaccard(Set<T> a, Set<T> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<T> small = a.size() <= b.size() ? a : b;
        Set<T> large = small == a ? b : a;
        int inter = 0;
        for (T t : small) if (large.contains(t)) inter++;
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0 : (double) inter / union;
    }

    /** Fraction of query tokens that appear in the document tokens. */
    public static double coverage(Set<String> queryTokens, Set<String> docTokens) {
        if (queryTokens.isEmpty()) return 0;
        long hit = queryTokens.stream().filter(docTokens::contains).count();
        return (double) hit / queryTokens.size();
    }

    public static List<String> topKeywords(String text, int n) {
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tokens(text)) {
            if (t.length() < 4 || t.chars().allMatch(Character::isDigit)) continue;
            counts.merge(t, 1, Integer::sum);
        }
        TopK<String> top = new TopK<>(Math.max(1, n));
        counts.forEach((word, c) -> top.offer(word, c + word.length() / 100.0));
        return top.toSortedList().stream().map(TopK.Entry::item).toList();
    }

    public static Set<String> hashtags(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) return out;
        Matcher m = HASHTAG.matcher(text);
        while (m.find()) out.add(m.group(1).toLowerCase(Locale.ROOT));
        return out;
    }

    public static String truncate(String s, int max) {
        if (s == null) return "";
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    public static List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : text.split("(?<=[.!?])\\s+|\\n+")) {
            String t = s.trim();
            if (t.length() > 3) out.add(t);
        }
        return out;
    }
}
