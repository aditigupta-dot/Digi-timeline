package com.digitaltimeline.ai;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.insight.InsightsReport;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Deterministic, extractive writing used when no LLM is available (or the LLM call fails). */
public final class OfflineNarrator {
    private OfflineNarrator() {}

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    public static String answer(String question, List<ContextItem> items, String rangeLabel, ZoneId zone) {
        if (items.isEmpty()) {
            return "I couldn't find anything in your timeline" + (rangeLabel == null ? "" : " for " + rangeLabel)
                    + " that matches “" + question + "”. Try different words or a wider time window.";
        }
        long projects = items.stream().map(i -> i.activity().getProject()).filter(p -> p != null && !p.isBlank())
                .distinct().count();
        StringBuilder sb = new StringBuilder();
        sb.append("Here is what your timeline says about “").append(question).append("”");
        if (rangeLabel != null) sb.append(" (").append(rangeLabel).append(")");
        sb.append(": ").append(items.size()).append(" related activities");
        if (projects > 0) sb.append(" across ").append(projects).append(projects == 1 ? " project" : " projects");
        sb.append(".\n\n");

        Map<java.time.LocalDate, List<ContextItem>> byDay = items.stream().collect(Collectors.groupingBy(
                i -> i.activity().getOccurredAt().atZone(zone).toLocalDate(), TreeMap::new, Collectors.toList()));
        byDay.forEach((day, list) -> {
            sb.append("**").append(DAY.format(day)).append("**\n");
            for (ContextItem item : list) {
                Activity a = item.activity();
                sb.append("- ").append(TIME.format(a.getOccurredAt().atZone(zone))).append(" · ")
                        .append(a.getType()).append(" · ").append(a.getTitle())
                        .append(" [").append(item.ref()).append("]\n");
            }
        });
        sb.append("\n_Offline mode: no language model is connected, so this is a ranked extract of your own activities._");
        return sb.toString();
    }

    public static String digest(InsightsReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("In the last ").append(r.days()).append(" days you logged ").append(r.total()).append(" activities on ")
                .append(r.activeDays()).append(" active days");
        if (r.changeVsPrevious() != null) {
            sb.append(String.format(Locale.ENGLISH, " (%+.0f%% vs the previous period)", r.changeVsPrevious()));
        }
        sb.append(".\n\n");
        if (!r.byProject().isEmpty()) {
            sb.append("Most of your attention went to ")
                    .append(r.byProject().entrySet().stream().limit(3)
                            .map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", ")))
                    .append(".\n");
        }
        sb.append("Your peak hour is ").append(String.format("%02d:00", r.peakHour())).append(" (")
                .append(r.workStyle().toLowerCase(Locale.ENGLISH)).append("), your longest streak is ")
                .append(r.longestStreak()).append(" days and your focus score is ").append(r.focusScore())
                .append("/100 (").append(r.contextSwitches()).append(" project switches).\n");
        if (!r.staleTasks().isEmpty()) {
            sb.append("\nStill open and aging: ")
                    .append(r.staleTasks().stream().limit(3).map(t -> "“" + t.title() + "”")
                            .collect(Collectors.joining(", ")))
                    .append(".");
        }
        return sb.toString();
    }
}
