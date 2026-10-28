package com.digitaltimeline.link;

import com.digitaltimeline.ai.AiProperties;
import com.digitaltimeline.core.VectorMath;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.RelationKind;
import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.repository.ActivityRepository;
import com.digitaltimeline.service.ActivityCreatedEvent;
import com.digitaltimeline.service.ActivityLinkedEvent;
import com.digitaltimeline.service.SearchIndex;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * The relationship engine. For every new activity it finds candidates (semantic neighbours + the previous step of
 * the same project), scores each pair with an explainable score and writes the strong ones into the knowledge graph.
 * Runs asynchronously on a virtual thread so saving an activity stays instant.
 */
@Service
public class LinkingService {
    private static final Logger log = LoggerFactory.getLogger(LinkingService.class);

    private final ActivityRepository repository;
    private final SearchIndex index;
    private final GraphService graph;
    private final ApplicationEventPublisher events;
    private final AiProperties ai;
    private final double minWeight;
    private final int maxLinks;

    public LinkingService(ActivityRepository repository, SearchIndex index, GraphService graph,
                          ApplicationEventPublisher events, AiProperties ai,
                          @Value("${dt.link.min-weight:0.33}") double minWeight,
                          @Value("${dt.link.max-links-per-activity:6}") int maxLinks) {
        this.repository = repository;
        this.index = index;
        this.graph = graph;
        this.events = events;
        this.ai = ai;
        this.minWeight = minWeight;
        this.maxLinks = maxLinks;
    }

    @Async("ingestExecutor")
    @EventListener
    public void onCreated(ActivityCreatedEvent event) {
        try {
            link(event.activityId());
        } catch (RuntimeException e) {
            log.warn("Linking failed for {}: {}", event.activityId(), e.getMessage());
        }
    }

    /** Public so the startup rebuild can call it for existing data. Returns the number of edges written. */
    public int link(UUID activityId) {
        Optional<Activity> found = repository.findById(activityId);
        if (found.isEmpty()) return 0;
        Activity a = found.get();
        graph.index(a);
        if (a.getEmbedding() == null) return 0;

        Map<UUID, Double> semantic = new LinkedHashMap<>();
        index.similarTo(a, Math.max(ai.topK(), 8), 0.10).forEach(h -> semantic.put(h.id(), h.semantic()));

        Optional<Activity> previous = a.getProject() == null ? Optional.empty()
                : repository.findFirstByUserIdAndProjectAndOccurredAtBeforeAndIdNotOrderByOccurredAtDesc(
                        a.getUserId(), a.getProject(), a.getOccurredAt(), a.getId());
        previous.ifPresent(p -> semantic.putIfAbsent(p.getId(),
                p.getEmbedding() == null ? 0 : VectorMath.cosine(a.getEmbedding(), p.getEmbedding())));

        Map<UUID, Activity> candidates = new HashMap<>();
        repository.findByIdInAndUserId(Set.copyOf(semantic.keySet()), a.getUserId())
                .forEach(c -> candidates.put(c.getId(), c));

        List<Edge> edges = semantic.entrySet().stream()
                .map(e -> {
                    Activity b = candidates.get(e.getKey());
                    if (b == null) return null;
                    RelationScorer.Score s = RelationScorer.score(a, b, e.getValue());
                    boolean isPrevious = previous.isPresent() && previous.get().getId().equals(b.getId());
                    if (isPrevious) {
                        long hours = Math.abs(Duration.between(b.getOccurredAt(), a.getOccurredAt()).toHours());
                        return new Edge(a.getId(), b.getId(), RelationKind.FOLLOWS_UP, Math.max(s.weight(), minWeight),
                                "previous step in “" + a.getProject() + "” (" + hours + "h earlier); " + s.reason());
                    }
                    return s.weight() >= minWeight ? new Edge(a.getId(), b.getId(), s.kind(), s.weight(), s.reason()) : null;
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(Edge::weight).reversed())
                .limit(maxLinks)
                .toList();

        edges.forEach(e -> {
            graph.index(candidates.get(e.to()));      // make sure both endpoints exist (idempotent)
            graph.link(e);
        });
        events.publishEvent(new ActivityLinkedEvent(a.getUserId(), a.getId(), edges.size()));
        return edges.size();
    }
}
