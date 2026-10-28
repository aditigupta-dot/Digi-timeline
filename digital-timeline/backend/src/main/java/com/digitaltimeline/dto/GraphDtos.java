package com.digitaltimeline.dto;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.RelationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GraphDtos {
    private GraphDtos() {}

    public record Node(UUID id, String title, ActivityType type, Instant occurredAt, String project) {}

    public record Edge(UUID from, UUID to, RelationKind kind, double weight, String reason) {}

    public record GraphView(List<Node> nodes, List<Edge> edges, String backend) {}

    public record Cluster(String label, List<UUID> members, List<String> titles, Instant start, Instant end,
                          String project, List<String> keywords) {}

    public record RelatedActivity(ActivityResponse activity, RelationKind kind, double weight, String reason) {}
}
