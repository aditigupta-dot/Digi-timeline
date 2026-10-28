package com.digitaltimeline.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chain-of-responsibility privacy filter. Every rule runs in order over the text.
 * Applied to everything that leaves the machine for an external LLM.
 */
public final class PiiRedactor {

    public record Rule(String name, Pattern pattern) {
        String placeholder() {
            return "[" + name + "]";
        }
    }

    public record Redacted(String text, Map<String, Integer> counts) {
        public int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private final List<Rule> chain;

    public PiiRedactor(List<Rule> chain) {
        this.chain = List.copyOf(chain);
    }

    public static PiiRedactor withDefaults() {
        return new PiiRedactor(List.of(
                new Rule("EMAIL", Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+")),
                new Rule("GITHUB_TOKEN", Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{20,}\\b")),
                new Rule("API_KEY", Pattern.compile("\\b(?:sk|pk)-[A-Za-z0-9_-]{16,}\\b")),
                new Rule("AWS_KEY", Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b")),
                new Rule("AADHAAR", Pattern.compile("\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b")),
                new Rule("CARD", Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b")),
                new Rule("PHONE", Pattern.compile(
                        "(?<![\\w])(?:\\+?\\d{1,3}[ -]?)?(?:\\d{10}|\\d{5}[ -]\\d{5}|\\(?\\d{3}\\)?[ -]\\d{3}[ -]\\d{4})(?![\\w])"))
        ));
    }

    public Redacted redact(String input) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (input == null || input.isEmpty()) return new Redacted("", counts);
        String text = input;
        for (Rule rule : chain) {
            var matcher = rule.pattern().matcher(text);
            long hits = matcher.results().count();
            if (hits > 0) {
                counts.put(rule.name(), (int) hits);
                text = rule.pattern().matcher(text).replaceAll(Matcher.quoteReplacement(rule.placeholder()));
            }
        }
        return new Redacted(text, counts);
    }
}
