package com.digitaltimeline.insight;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.repository.UserRepository;
import com.digitaltimeline.service.ActivityService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** The AI writes its weekly review INTO the timeline, so the digest becomes searchable history too. */
@Service
public class DigestService {
    private static final Logger log = LoggerFactory.getLogger(DigestService.class);

    private final InsightService insights;
    private final ActivityService activities;
    private final UserRepository users;
    private final Clock clock;

    public DigestService(InsightService insights, ActivityService activities, UserRepository users, Clock clock) {
        this.insights = insights;
        this.activities = activities;
        this.users = users;
        this.clock = clock;
    }

    public Optional<ActivityResponse> createWeeklyDigest(UUID userId) {
        InsightsReport report = insights.report(userId, 7, ZoneOffset.UTC);
        String text = insights.narrative(userId, report);
        LocalDate weekStart = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ActivityRequest request = new ActivityRequest(ActivityType.NOTE, "Weekly digest · week of " + weekStart, text,
                clock.instant(), "Digest", Set.of("digest", "ai-generated"), Map.of("format", "markdown"));
        return activities.ingest(userId, request, "ai-digest", "digest:" + weekStart).map(ActivityResponse::from);
    }

    @Scheduled(cron = "0 0 8 * * MON")
    public void weeklyForEveryone() {
        for (User u : users.findAll()) {
            try {
                createWeeklyDigest(u.getId());
            } catch (RuntimeException e) {
                log.warn("Digest failed for {}: {}", u.getEmail(), e.getMessage());
            }
        }
    }
}
