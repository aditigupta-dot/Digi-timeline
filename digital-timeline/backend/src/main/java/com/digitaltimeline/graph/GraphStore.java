package com.digitaltimeline.graph;

import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.dto.GraphDtos.Node;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** STRATEGY: the knowledge graph can live in Neo4j or, for zero-setup demos, in memory. */
public interface GraphStore {

    String backend();

    void upsertNode(UUID userId, Node node);

    void upsertEdge(Edge edge);

    /** Edges touching the node, strongest first. "from" is always the queried node. */
    List<Edge> edgesOf(UUID nodeId, int limit);

    List<Node> nodesOf(UUID userId, Instant from, Instant to);

    /** Each undirected edge once, strongest first. */
    List<Edge> edgesOf(UUID userId, Instant from, Instant to, int limit);

    /** Ids along the shortest path (inclusive), or an empty list when the nodes are not connected. */
    List<UUID> shortestPath(UUID from, UUID to);

    void deleteNode(UUID nodeId);
}
