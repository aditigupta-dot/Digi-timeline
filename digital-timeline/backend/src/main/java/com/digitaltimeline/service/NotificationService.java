package com.digitaltimeline.service;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.EventActivity;
import com.digitaltimeline.domain.TaskActivity;
import com.digitaltimeline.dto.DashboardDtos.Notice;
import com.digitaltimeline.dto.DashboardDtos.NoticeKind;
import com.digitaltimeline.repository.ActivityRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Builds the reminders shown in the bell: overdue tasks, backlog (open for 7+ days), tasks due within 48 hours
 * and events starting within 7 days. Computed on demand, so it is always up to date.
 */
@Service
public class NotificationService {

    private final ActivityRepository repository;
    private final Clock clock;

    public NotificationService(ActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Notice> notices(UUID userId) {
        Instant now = clock.instant();
        List<Notice> out = new ArrayList<>();
        for (Activity a : repository.findByUserId(userId)) {
            if (a instanceof TaskActivity t) {
                if (t.getStatus() == null || !t.getStatus().isOpen()) continue;
                long ageDays = Duration.between(t.getOccurredAt(), now).toDays();
                if (t.getDueAt() != null && t.getDueAt().isBefore(now)) {
                    long late = Duration.between(t.getDueAt(), now).toDays();
                    String how = late <= 0 ? "less than a day" : late + (late == 1 ? " day" : " days");
                    out.add(new Notice(t.getId(), NoticeKind.OVERDUE, "high", t.getTitle(),
                            "Overdue by " + how, t.getDueAt(), t.getProject()));
                } else if (t.getDueAt() != null && t.getDueAt().isBefore(now.plus(48, ChronoUnit.HOURS))) {
                    out.add(new Notice(t.getId(), NoticeKind.DUE_SOON, "medium", t.getTitle(),
                            "Due " + inText(Duration.between(now, t.getDueAt())), t.getDueAt(), t.getProject()));
                } else if (ageDays >= 7) {
                    out.add(new Notice(t.getId(), NoticeKind.BACKLOG, "medium", t.getTitle(),
                            "Still pending after " + ageDays + " days", t.getOccurredAt(), t.getProject()));
                }
            } else if (a instanceof EventActivity e) {
                if (e.getOccurredAt().isAfter(now) && e.getOccurredAt().isBefore(now.plus(7, ChronoUnit.DAYS))) {
                    out.add(new Notice(e.getId(), NoticeKind.EVENT_SOON, "info", e.getTitle(),
                            "Starts " + inText(Duration.between(now, e.getOccurredAt())), e.getOccurredAt(), e.getProject()));
                }
            }
        }
        out.sort(Comparator.comparingInt((Notice n) -> rank(n.severity())).thenComparing(Notice::when));
        return out;
    }

    private static int rank(String severity) {
        return switch (severity) {
            case "high" -> 0;
            case "medium" -> 1;
            default -> 2;
        };
    }

    static String inText(Duration d) {
        long hours = d.toHours();
        if (hours < 1) return "within the hour";
        if (hours < 24) return "in " + hours + (hours == 1 ? " hour" : " hours");
        long days = d.toDays();
        return "in " + days + (days == 1 ? " day" : " days");
    }
}
