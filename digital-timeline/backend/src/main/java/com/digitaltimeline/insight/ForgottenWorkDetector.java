package com.digitaltimeline.insight;

import com.digitaltimeline.core.TopK;
import com.digitaltimeline.core.VectorMath;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.TaskActivity;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.repository.ActivityRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Unique feature: finds open tasks you stopped touching that are still SEMANTICALLY CLOSE to what you did this week.
 * Score = relevance to recent work (centroid of the last 7 days' embeddings) + age + overdue bonus.
 */
@Service
public class ForgottenWorkDetector {

    private final ActivityRepository repository;
    private final Clock clock;

    public ForgottenWorkDetector(ActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ForgottenWork> detect(UUID userId, int limit) {
        Instant now = clock.instant();
        List<Activity> all = repository.findByUserId(userId);

        List<float[]> recent = all.stream()
                .filter(a -> a.getEmbedding() != null && a.getOccurredAt().isAfter(now.minus(7, ChronoUnit.DAYS)))
                .map(Activity::getEmbedding)
                .toList();
        int dims = recent.isEmpty() ? 0 : recent.get(0).length;
        float[] centroid = dims == 0 ? new float[0] : VectorMath.centroid(recent, dims);

        TopK<ForgottenWork> top = new TopK<>(Math.max(1, limit));
        all.stream()
                .filter(a -> a instanceof TaskActivity t && t.getStatus() != null && t.getStatus().isOpen())
                .filter(a -> a.getOccurredAt().isBefore(now.minus(5, ChronoUnit.DAYS)))
                .forEach(a -> {
                    TaskActivity t = (TaskActivity) a;
                    long staleDays = Duration.between(a.getOccurredAt(), now).toDays();
                    double relevance = a.getEmbedding() != null && a.getEmbedding().length == dims && dims > 0
                            ? Math.max(0, VectorMath.cosine(a.getEmbedding(), centroid)) : 0;
                    boolean overdue = t.isOverdue(now);
                    double score = 0.5 * relevance + 0.3 * Math.min(1.0, staleDays / 30.0) + (overdue ? 0.2 : 0);
                    String reason = "Open for " + staleDays + " days"
                            + (overdue ? " and overdue" : "")
                            + (relevance >= 0.15 ? "; closely related to what you worked on this week ("
                            + Math.round(relevance * 100) + "% match)" : "");
                    top.offer(new ForgottenWork(ActivityResponse.from(a), score, reason), score);
                });
        return top.toSortedList().stream().map(TopK.Entry::item).toList();
    }
}
