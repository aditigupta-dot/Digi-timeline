package com.digitaltimeline.core;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Understands time phrases inside natural-language questions:
 * "yesterday", "last week", "this month", "last 10 days", "in March" ...
 * This is how "what did I do last week on the thesis?" becomes a date filter before the AI is even called.
 */
public final class TimeRangeParser {

    private static final Pattern LAST_N =
            Pattern.compile("\\b(?:last|past|previous)\\s+(\\d{1,3})\\s+(day|days|week|weeks|month|months)\\b");
    private static final Pattern IN_MONTH = Pattern.compile(
            "\\b(?:in|during|for)\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\b");

    private final Clock clock;
    private final ZoneId zone;

    public TimeRangeParser(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    public Optional<TimeRange> parse(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String q = text.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(clock.withZone(zone));

        Matcher m = LAST_N.matcher(q);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            LocalDate start = unit.startsWith("day") ? today.minusDays(n)
                    : unit.startsWith("week") ? today.minusWeeks(n) : today.minusMonths(n);
            return Optional.of(range(start, today.plusDays(1), "last " + n + " " + unit));
        }
        if (q.contains("yesterday")) return Optional.of(range(today.minusDays(1), today, "yesterday"));
        if (q.contains("today")) return Optional.of(range(today, today.plusDays(1), "today"));
        if (q.contains("last week")) {
            LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return Optional.of(range(thisMonday.minusWeeks(1), thisMonday, "last week"));
        }
        if (q.contains("this week")) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return Optional.of(range(monday, monday.plusWeeks(1), "this week"));
        }
        if (q.contains("last month")) {
            LocalDate first = today.withDayOfMonth(1).minusMonths(1);
            return Optional.of(range(first, first.plusMonths(1), "last month"));
        }
        if (q.contains("this month")) {
            LocalDate first = today.withDayOfMonth(1);
            return Optional.of(range(first, first.plusMonths(1), "this month"));
        }
        if (q.contains("last year")) {
            LocalDate first = today.withDayOfYear(1).minusYears(1);
            return Optional.of(range(first, first.plusYears(1), "last year"));
        }
        if (q.contains("this year")) {
            LocalDate first = today.withDayOfYear(1);
            return Optional.of(range(first, first.plusYears(1), "this year"));
        }
        Matcher mm = IN_MONTH.matcher(q);
        if (mm.find()) {
            Month month = Month.valueOf(mm.group(1).toUpperCase(Locale.ROOT));
            int year = month.getValue() > today.getMonthValue() ? today.getYear() - 1 : today.getYear();
            LocalDate first = LocalDate.of(year, month, 1);
            return Optional.of(range(first, first.plusMonths(1),
                    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year));
        }
        return Optional.empty();
    }

    private TimeRange range(LocalDate from, LocalDate toExclusive, String label) {
        return new TimeRange(from.atStartOfDay(zone).toInstant(), toExclusive.atStartOfDay(zone).toInstant(), label);
    }
}
