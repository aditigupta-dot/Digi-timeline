package com.digitaltimeline.insight;

import com.digitaltimeline.ai.AiProperties;
import com.digitaltimeline.ai.LlmClient;
import com.digitaltimeline.ai.OfflineNarrator;
import com.digitaltimeline.core.LruCache;
import com.digitaltimeline.core.PiiRedactor;
import com.digitaltimeline.core.TopK;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.exception.AiProviderException;
import com.digitaltimeline.repository.ActivityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Productivity analytics computed entirely with streams and collectors, plus an AI-written narrative. */
@Service
public class InsightService {
    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    private final ActivityRepository repository;
    private final ForgottenWorkDetector forgotten;
    private final LlmClient llm;
    private final PiiRedactor redactor;
    private final AiProperties ai;
    private final Clock clock;
    private final LruCache<String, String> narrativeCache = new LruCache<>(64);

    public InsightService(ActivityRepository repository, ForgottenWorkDetector forgotten, LlmClient llm,
                          PiiRedactor redactor, AiProperties ai, Clock clock) {
        this.repository = repository;
        this.forgotten = forgotten;
        this.llm = llm;
        this.redactor = redactor;
        this.ai = ai;
        this.clock = clock;
    }

    public InsightsReport report(UUID userId, int days, ZoneId zone) {
        Instant now = clock.instant();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        Instant prevFrom = from.minus(days, ChronoUnit.DAYS);
        Instant to = now.plus(1, ChronoUnit.DAYS);

        List<Activity> current = repository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(userId, from, to);
        long previousCount = repository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(userId, prevFrom, from).size();

        Map<ActivityType, Long> byType = current.stream().collect(
                Collectors.groupingBy(Activity::getType, () -> new EnumMap<>(ActivityType.class), Collectors.counting()));

        Map<String, Long> byProject = current.stream()
                .filter(a -> a.getProject() != null && !a.getProject().isBlank())
                .collect(Collectors.groupingBy(Activity::getProject, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, LinkedHashMap::new));

        TreeMap<LocalDate, Long> perDayMap = current.stream().collect(Collectors.groupingBy(
                a -> a.getOccurredAt().atZone(zone).toLocalDate(), TreeMap::new, Collectors.counting()));
        List<InsightsReport.DayCount> perDay = perDayMap.entrySet().stream()
                .map(e -> new InsightsReport.DayCount(e.getKey(), e.getValue())).toList();

        Map<Integer, Long> byHour = current.stream().collect(
                Collectors.groupingBy(a -> a.getOccurredAt().atZone(zone).getHour(), Collectors.counting()));
        int peakHour = byHour.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(12);

        String busiest = perDayMap.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + " activities)").orElse("-");

        // streaks over consecutive active days
        TreeSet<LocalDate> active = new TreeSet<>(perDayMap.keySet());
        int longest = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : active) {
            run = prev != null && prev.plusDays(1).equals(d) ? run + 1 : 1;
            longest = Math.max(longest, run);
            prev = d;
        }
        LocalDate today = LocalDate.now(clock.withZone(zone));
        int currentStreak = 0;
        LocalDate cursor = active.contains(today) ? today : today.minusDays(1);
        while (active.contains(cursor)) {
            currentStreak++;
            cursor = cursor.minusDays(1);
        }

        // context switching: how often consecutive activities jump between projects
        List<String> projectSequence = current.stream()
                .sorted(Comparator.comparing(Activity::getOccurredAt))
                .map(Activity::getProject).filter(p -> p != null && !p.isBlank()).toList();
        int switches = 0;
        for (int i = 1; i < projectSequence.size(); i++) {
            if (!projectSequence.get(i).equalsIgnoreCase(projectSequence.get(i - 1))) switches++;
        }
        int focus = projectSequence.size() < 2 ? 100
                : (int) Math.round((1.0 - (double) switches / (projectSequence.size() - 1)) * 100);

        Map<String, Integer> tagCounts = new HashMap<>();
        current.forEach(a -> a.getTags().forEach(t -> tagCounts.merge(t, 1, Integer::sum)));
        TopK<String> topTags = new TopK<>(6);
        tagCounts.forEach((tag, c) -> topTags.offer(tag, c));

        Double change = previousCount == 0 ? null : (current.size() - previousCount) * 100.0 / previousCount;

        return new InsightsReport(days, current.size(), byType, byProject, perDay, peakHour, workStyle(peakHour),
                active.size(), longest, currentStreak, focus, switches,
                topTags.toSortedList().stream().map(TopK.Entry::item).toList(),
                forgotten.detect(userId, 5).stream().map(ForgottenWork::task).toList(), change, busiest);
    }

    public String narrative(UUID userId, InsightsReport r) {
        String key = userId + ":" + r.days() + ":" + r.total() + ":" + r.activeDays() + ":" + llm.name();
        return narrativeCache.computeIfAbsent(key, k -> writeNarrative(r));
    }

    private String writeNarrative(InsightsReport r) {
        if (!llm.isGenerative()) return OfflineNarrator.digest(r);
        String facts = """
                Period: last %d days. Activities: %d on %d active days. Change vs previous period: %s.
                By type: %s. Top projects: %s. Peak hour: %02d:00 (%s). Longest streak: %d days. Current streak: %d.
                Focus score: %d/100 (%d project switches). Top tags: %s. Busiest day: %s.
                Open tasks going stale: %s.
                """.formatted(r.days(), r.total(), r.activeDays(),
                r.changeVsPrevious() == null ? "n/a" : String.format("%+.0f%%", r.changeVsPrevious()),
                r.byType(), r.byProject(), r.peakHour(), r.workStyle(), r.longestStreak(), r.currentStreak(),
                r.focusScore(), r.contextSwitches(), r.topTags(), r.busiestDay(),
                r.staleTasks().stream().map(t -> t.title()).toList());
        if (ai.redactPii()) facts = redactor.redact(facts).text();
        String system = "You write short, encouraging but honest productivity reviews for one person. Use ONLY the "
                + "numbers given. 120 words max. Plain paragraphs, no headings. End with ONE concrete suggestion.";
        try {
            return llm.complete(system, facts);
        } catch (AiProviderException e) {
            log.warn("Narrative LLM failed, using offline text: {}", e.getMessage());
            return OfflineNarrator.digest(r);
        }
    }

    static String workStyle(int peakHour) {
        if (peakHour >= 22 || peakHour < 4) return "Night owl";
        if (peakHour < 9) return "Early bird";
        if (peakHour < 17) return "Daytime";
        return "Evening";
    }
}
