package com.digitaltimeline.graph;

import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.dto.GraphDtos.Node;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe adjacency-map graph. Same behaviour as Neo4jGraphStore, no database needed. */
public class InMemoryGraphStore implements GraphStore {

    private final Map<UUID, Node> nodes = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> owners = new ConcurrentHashMap<>();
    // node -> (otherId|kind -> edge as seen from node)
    private final Map<UUID, Map<String, Edge>> adjacency = new ConcurrentHashMap<>();

    @Override
    public String backend() {
        return "memory";
    }

    @Override
    public void upsertNode(UUID userId, Node node) {
        nodes.put(node.id(), node);
        owners.put(node.id(), userId);
        adjacency.computeIfAbsent(node.id(), k -> new ConcurrentHashMap<>());
    }

    @Override
    public void upsertEdge(Edge edge) {
        if (!nodes.containsKey(edge.from()) || !nodes.containsKey(edge.to())) return;
        adjacency.computeIfAbsent(edge.from(), k -> new ConcurrentHashMap<>()).put(edge.to() + "|" + edge.kind(), edge);
        Edge mirrored = new Edge(edge.to(), edge.from(), edge.kind(), edge.weight(), edge.reason());
        adjacency.computeIfAbsent(edge.to(), k -> new ConcurrentHashMap<>()).put(edge.from() + "|" + edge.kind(), mirrored);
    }

    @Override
    public List<Edge> edgesOf(UUID nodeId, int limit) {
        return adjacency.getOrDefault(nodeId, Map.of()).values().stream()
                .sorted(Comparator.comparingDouble(Edge::weight).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Node> nodesOf(UUID userId, Instant from, Instant to) {
        return nodes.values().stream()
                .filter(n -> userId.equals(owners.get(n.id())))
                .filter(n -> !n.occurredAt().isBefore(from) && !n.occurredAt().isAfter(to))
                .sorted(Comparator.comparing(Node::occurredAt))
                .toList();
    }

    @Override
    public List<Edge> edgesOf(UUID userId, Instant from, Instant to, int limit) {
        Map<UUID, Node> visible = new HashMap<>();
        nodesOf(userId, from, to).forEach(n -> visible.put(n.id(), n));
        List<Edge> out = new ArrayList<>();
        for (UUID id : visible.keySet()) {
            for (Edge e : adjacency.getOrDefault(id, Map.of()).values()) {
                if (e.from().compareTo(e.to()) < 0 && visible.containsKey(e.to())) out.add(e);
            }
        }
        out.sort(Comparator.comparingDouble(Edge::weight).reversed());
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    @Override
    public List<UUID> shortestPath(UUID from, UUID to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) return List.of();
        Map<UUID, UUID> previous = new HashMap<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(from);
        previous.put(from, from);
        while (!queue.isEmpty()) {
            UUID cur = queue.poll();
            if (cur.equals(to)) break;
            for (Edge e : adjacency.getOrDefault(cur, Map.of()).values()) {
                if (previous.putIfAbsent(e.to(), cur) == null) queue.add(e.to());
            }
        }
        if (!previous.containsKey(to)) return List.of();
        List<UUID> path = new ArrayList<>();
        for (UUID at = to; ; at = previous.get(at)) {
            path.add(at);
            if (at.equals(from)) break;
        }
        Collections.reverse(path);
        return path;
    }

    @Override
    public void deleteNode(UUID nodeId) {
        nodes.remove(nodeId);
        owners.remove(nodeId);
        Map<String, Edge> own = adjacency.remove(nodeId);
        if (own != null) {
            own.values().forEach(e -> {
                Map<String, Edge> other = adjacency.get(e.to());
                if (other != null) other.keySet().removeIf(k -> k.startsWith(nodeId + "|"));
            });
        }
    }
}
