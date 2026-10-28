package com.digitaltimeline.service;

import com.digitaltimeline.ai.EmbeddingProvider;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.exception.DuplicateResourceException;
import com.digitaltimeline.exception.DigitalTimelineException;
import com.digitaltimeline.exception.ResourceNotFoundException;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.ingest.ImportanceScorer;
import com.digitaltimeline.ingest.IngestionPipeline;
import com.digitaltimeline.repository.ActivityRepository;
import com.digitaltimeline.repository.AnalyticsJdbcDao;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository repository;
    private final ActivityFactory factory;
    private final IngestionPipeline pipeline;
    private final SearchIndex index;
    private final GraphService graph;
    private final EmbeddingProvider embeddings;
    private final AnalyticsJdbcDao jdbc;
    private final ApplicationEventPublisher events;

    public ActivityService(ActivityRepository repository, ActivityFactory factory, IngestionPipeline pipeline,
                           SearchIndex index, GraphService graph, EmbeddingProvider embeddings,
                           AnalyticsJdbcDao jdbc, ApplicationEventPublisher events) {
        this.repository = repository;
        this.factory = factory;
        this.pipeline = pipeline;
        this.index = index;
        this.graph = graph;
        this.embeddings = embeddings;
        this.jdbc = jdbc;
        this.events = events;
    }

    public ActivityResponse create(UUID userId, ActivityRequest request) {
        return ingest(userId, request, "manual", null)
                .map(ActivityResponse::from)
                .orElseThrow(() -> new DuplicateResourceException("Activity already exists"));
    }

    /** The one entry point for ALL activities (manual, GitHub, files, calendar, imports). Empty = duplicate skipped. */
    public Optional<Activity> ingest(UUID userId, ActivityRequest request, String source, String externalId) {
        if (externalId != null && repository.existsByUserIdAndSourceAndExternalId(userId, source, externalId)) {
            return Optional.empty();
        }
        Activity activity = factory.create(userId, request, source, externalId);
        pipeline.run(activity);
        Activity saved = repository.save(activity);
        index.put(saved);
        events.publishEvent(new ActivityCreatedEvent(userId, saved.getId(), ActivityResponse.from(saved)));
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(UUID userId, Instant from, Instant to, Set<ActivityType> types, String project) {
        return repository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(userId, from, to).stream()
                .filter(a -> types == null || types.isEmpty() || types.contains(a.getType()))
                .filter(a -> project == null || project.isBlank() || project.equalsIgnoreCase(a.getProject()))
                .map(ActivityResponse::from)
                .toList();
    }

    public Activity getEntity(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Activity", id));
    }

    public ActivityResponse get(UUID userId, UUID id) {
        return ActivityResponse.from(getEntity(userId, id));
    }

    public ActivityResponse update(UUID userId, UUID id, ActivityRequest request) {
        Activity a = getEntity(userId, id);
        if (a.getType() != request.type()) {
            throw new DigitalTimelineException("Activity type cannot be changed", 400);
        }
        factory.apply(a, request);
        pipeline.run(a);
        Activity saved = repository.save(a);
        index.put(saved);
        events.publishEvent(new ActivityCreatedEvent(userId, saved.getId(), ActivityResponse.from(saved)));
        return ActivityResponse.from(saved);
    }

    public void delete(UUID userId, UUID id) {
        Activity a = getEntity(userId, id);
        repository.delete(a);
        index.remove(id);
        graph.remove(id);
    }

    /** Re-embeds everything (after switching embedding provider) and refreshes importance with one JDBC batch. */
    public int reindex(UUID userId) {
        List<Activity> all = repository.findByUserId(userId);
        Map<UUID, Double> importance = new HashMap<>();
        for (Activity a : all) {
            try {
                a.setEmbedding(embeddings.embed(a.searchableText()));
            } catch (RuntimeException e) {
                a.setEmbedding(null);
            }
            a.setImportance(ImportanceScorer.score(a));
            importance.put(a.getId(), a.getImportance());
        }
        repository.saveAll(all);
        all.forEach(index::put);
        try {
            jdbc.batchUpdateImportance(userId, importance);
        } catch (SQLException e) {
            throw new DigitalTimelineException("Importance refresh failed: " + e.getMessage(), 500, e);
        }
        return all.size();
    }

    public static String normaliseProject(String project) {
        return project == null ? null : project.trim().toLowerCase(Locale.ROOT);
    }
}
