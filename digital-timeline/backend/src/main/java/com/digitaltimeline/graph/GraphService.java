package com.digitaltimeline.graph;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.dto.GraphDtos.GraphView;
import com.digitaltimeline.dto.GraphDtos.Node;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * FACADE + resilience: every graph call is guarded, so a Neo4j outage never breaks the timeline or search.
 * The rest of the app only talks to this class.
 */
@Service
public class GraphService {
    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final GraphStore store;

    public GraphService(GraphStore store) {
        this.store = store;
    }

    public String backend() {
        return store.backend();
    }

    public static Node toNode(Activity a) {
        return new Node(a.getId(), a.getTitle(), a.getType(), a.getOccurredAt(), a.getProject());
    }

    public void index(Activity a) {
        guard("upsertNode", () -> { store.upsertNode(a.getUserId(), toNode(a)); return null; });
    }

    public void link(Edge edge) {
        guard("upsertEdge", () -> { store.upsertEdge(edge); return null; });
    }

    public List<Edge> neighbours(UUID id, int limit) {
        return guard("edgesOf", () -> store.edgesOf(id, limit), List.of());
    }

    public GraphView view(UUID userId, Instant from, Instant to, int maxEdges) {
        List<Node> nodes = guard("nodesOf", () -> store.nodesOf(userId, from, to), List.of());
        List<Edge> edges = guard("edgesOf", () -> store.edgesOf(userId, from, to, maxEdges), List.of());
        return new GraphView(nodes, edges, store.backend());
    }

    public List<UUID> path(UUID from, UUID to) {
        return guard("shortestPath", () -> store.shortestPath(from, to), List.of());
    }

    public void remove(UUID id) {
        guard("deleteNode", () -> { store.deleteNode(id); return null; });
    }

    private <T> T guard(String op, Supplier<T> call) {
        return guard(op, call, null);
    }

    private <T> T guard(String op, Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            log.warn("Graph operation {} failed: {}", op, e.getMessage());
            return fallback;
        }
    }
}
