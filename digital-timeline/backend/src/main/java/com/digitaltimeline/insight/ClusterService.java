package com.digitaltimeline.insight;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.core.UnionFind;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.dto.GraphDtos.Cluster;
import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.dto.GraphDtos.GraphView;
import com.digitaltimeline.dto.GraphDtos.Node;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.repository.ActivityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Detects "threads of work": connected groups of activities in the knowledge graph (union-find over strong links). */
@Service
public class ClusterService {

    private final GraphService graph;
    private final ActivityRepository repository;
    private final Clock clock;

    public ClusterService(GraphService graph, ActivityRepository repository, Clock clock) {
        this.graph = graph;
        this.repository = repository;
        this.clock = clock;
    }

    public List<Cluster> threads(UUID userId, int days, double minWeight) {
        Instant now = clock.instant();
        GraphView view = graph.view(userId, now.minus(days, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), 2000);
        UnionFind<UUID> uf = new UnionFind<>();
        view.nodes().forEach(n -> uf.add(n.id()));
        for (Edge e : view.edges()) {
            if (e.weight() >= minWeight) uf.union(e.from(), e.to());
        }
        Map<UUID, Node> nodes = view.nodes().stream().collect(Collectors.toMap(Node::id, n -> n, (a, b) -> a));

        return uf.groups().values().stream()
                .filter(g -> g.size() >= 2)
                .sorted(Comparator.comparingInt((List<UUID> g) -> g.size()).reversed())
                .limit(12)
                .map(group -> toCluster(userId, group, nodes))
                .toList();
    }

    private Cluster toCluster(UUID userId, List<UUID> group, Map<UUID, Node> nodes) {
        Map<UUID, Activity> activities = new HashMap<>();
        repository.findByIdInAndUserId(group, userId).forEach(a -> activities.put(a.getId(), a));
        List<Activity> members = activities.values().stream()
                .sorted(Comparator.comparing(Activity::getOccurredAt)).toList();

        String project = members.stream().map(Activity::getProject).filter(p -> p != null && !p.isBlank())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        String text = members.stream().map(a -> a.getTitle() + " " + (a.getContent() == null ? "" : a.getContent()))
                .collect(Collectors.joining(" "));
        List<String> keywords = TextTools.topKeywords(text, 4);
        String label = project != null ? project + ": " + String.join(", ", keywords.stream().limit(2).toList())
                : String.join(", ", keywords);
        return new Cluster(label, members.stream().map(Activity::getId).toList(),
                members.stream().map(Activity::getTitle).limit(6).toList(),
                members.isEmpty() ? null : members.get(0).getOccurredAt(),
                members.isEmpty() ? null : members.get(members.size() - 1).getOccurredAt(), project, keywords);
    }
}
