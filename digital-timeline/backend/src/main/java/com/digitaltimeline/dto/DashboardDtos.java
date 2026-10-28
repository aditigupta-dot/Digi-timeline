package com.digitaltimeline.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {}

    public enum NoticeKind { OVERDUE, BACKLOG, DUE_SOON, EVENT_SOON }

    /** One reminder. severity: high (overdue), medium (backlog / due soon), info (upcoming event). */
    public record Notice(UUID id, NoticeKind kind, String severity, String title, String message, Instant when,
                         String project) {}

    public record ScheduleItem(UUID id, LocalDate date, String time, String title, String kind, String project) {}

    public record ProjectCard(String name, long total, long open, Instant lastActivity, Map<String, Long> byType) {}

    public record TaskStats(long total, long done, long open, long overdue) {}

    public record Rings(int productivity, int focus, int consistency) {}

    public record Dashboard(String displayName, List<Notice> notices, List<ScheduleItem> upcomingEvents,
                            List<ScheduleItem> schedule, List<ProjectCard> projects, TaskStats tasks, Rings rings) {}
}
