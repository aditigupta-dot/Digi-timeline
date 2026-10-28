package com.digitaltimeline.service;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.EventActivity;
import com.digitaltimeline.domain.TaskActivity;
import com.digitaltimeline.domain.TaskStatus;
import com.digitaltimeline.dto.DashboardDtos.Dashboard;
import com.digitaltimeline.dto.DashboardDtos.ProjectCard;
import com.digitaltimeline.dto.DashboardDtos.Rings;
import com.digitaltimeline.dto.DashboardDtos.ScheduleItem;
import com.digitaltimeline.dto.DashboardDtos.TaskStats;
import com.digitaltimeline.insight.InsightService;
import com.digitaltimeline.insight.InsightsReport;
import com.digitaltimeline.repository.ActivityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Everything the dashboard page needs, in one call. */
@Service
public class DashboardService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ActivityRepository repository;
    private final NotificationService notifications;
    private final InsightService insights;
    private final AuthService auth;
    private final Clock clock;

    public DashboardService(ActivityRepository repository, NotificationService notifications,
                            InsightService insights, AuthService auth, Clock clock) {
        this.repository = repository;
        this.notifications = notifications;
        this.insights = insights;
        this.auth = auth;
        this.clock = clock;
    }

    public Dashboard build(UUID userId, ZoneId zone) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(30, ChronoUnit.DAYS);
        Instant windowEnd = now.plus(90, ChronoUnit.DAYS);
        List<Activity> all = repository.findByUserId(userId);

        long total = 0, done = 0, overdue = 0;
        List<ScheduleItem> schedule = new ArrayList<>();
        List<ScheduleItem> upcoming = new ArrayList<>();
        Map<String, long[]> projectCounts = new LinkedHashMap<>();       // total, open
        Map<String, Instant> projectLast = new LinkedHashMap<>();
        Map<String, Map<String, Long>> projectTypes = new LinkedHashMap<>();

        for (Activity a : all) {
            if (a instanceof TaskActivity t) {
                total++;
                boolean open = t.getStatus() != null && t.getStatus().isOpen();
                if (t.getStatus() == TaskStatus.DONE) done++;
                if (t.isOverdue(now)) overdue++;
                if (open && t.getDueAt() != null && t.getDueAt().isAfter(windowStart) && t.getDueAt().isBefore(windowEnd)) {
                    schedule.add(item(t.getId(), t.getDueAt(), zone, "Due: " + t.getTitle(), "TASK_DUE", t.getProject()));
                }
            } else if (a instanceof EventActivity e) {
                if (e.getOccurredAt().isAfter(windowStart) && e.getOccurredAt().isBefore(windowEnd)) {
                    ScheduleItem si = item(e.getId(), e.getOccurredAt(), zone, e.getTitle(), "EVENT", e.getProject());
                    schedule.add(si);
                    if (e.getOccurredAt().isAfter(now)) upcoming.add(si);
                }
            }
            if (a.getProject() != null && !a.getProject().isBlank()) {
                long[] counts = projectCounts.computeIfAbsent(a.getProject(), k -> new long[2]);
                counts[0]++;
                if (a instanceof TaskActivity t && t.getStatus() != null && t.getStatus().isOpen()) counts[1]++;
                projectLast.merge(a.getProject(), a.getOccurredAt(), (x, y) -> x.isAfter(y) ? x : y);
                projectTypes.computeIfAbsent(a.getProject(), k -> new LinkedHashMap<>())
                        .merge(a.getType().name(), 1L, Long::sum);
            }
        }

        Comparator<ScheduleItem> byWhen = Comparator.comparing(ScheduleItem::date).thenComparing(ScheduleItem::time);
        schedule.sort(byWhen);
        upcoming.sort(byWhen);

        List<ProjectCard> projects = new ArrayList<>();
        projectCounts.forEach((name, c) -> projects.add(
                new ProjectCard(name, c[0], c[1], projectLast.get(name), projectTypes.get(name))));
        projects.sort(Comparator.comparing(ProjectCard::lastActivity).reversed());

        InsightsReport report = insights.report(userId, 30, zone);
        int productivity = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        int consistency = (int) Math.min(100, Math.round(report.activeDays() * 100.0 / 30));

        return new Dashboard(auth.require(userId).getDisplayName(), notifications.notices(userId),
                upcoming.size() > 5 ? upcoming.subList(0, 5) : upcoming, schedule, projects,
                new TaskStats(total, done, total - done, overdue),
                new Rings(productivity, report.focusScore(), consistency));
    }

    private static ScheduleItem item(UUID id, Instant when, ZoneId zone, String title, String kind, String project) {
        ZonedDateTime z = when.atZone(zone);
        return new ScheduleItem(id, z.toLocalDate(), TIME.format(z), title, kind, project);
    }
}
