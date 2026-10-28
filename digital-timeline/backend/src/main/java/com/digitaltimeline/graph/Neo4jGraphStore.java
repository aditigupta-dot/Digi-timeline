package com.digitaltimeline.graph;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.RelationKind;
import com.digitaltimeline.dto.GraphDtos.Edge;
import com.digitaltimeline.dto.GraphDtos.Node;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Knowledge graph in Neo4j. Activities are (:Activity) nodes, links are [:RELATED {kind, weight, reason}].
 * Uses the official driver directly, so there is no clash with JPA's transaction manager.
 */
public class Neo4jGraphStore implements GraphStore {
    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphStore.class);

    private final Driver driver;

    public Neo4jGraphStore(Driver driver) {
        this.driver = driver;
    }

    @PostConstruct
    void init() {
        try (Session s = driver.session()) {
            s.run("CREATE CONSTRAINT activity_id IF NOT EXISTS FOR (a:Activity) REQUIRE a.id IS UNIQUE").consume();
            s.run("CREATE INDEX activity_user IF NOT EXISTS FOR (a:Activity) ON (a.userId)").consume();
        } catch (RuntimeException e) {
            log.warn("Neo4j not reachable yet ({}). Graph features degrade until it is.", e.getMessage());
        }
    }

    @Override
    public String backend() {
        return "neo4j";
    }

    @Override
    public void upsertNode(UUID userId, Node n) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", n.id().toString());
        p.put("userId", userId.toString());
        p.put("title", n.title());
        p.put("type", n.type().name());
        p.put("ts", n.occurredAt().toEpochMilli());
        p.put("project", n.project());
        run("MERGE (a:Activity {id:$id}) SET a.userId=$userId, a.title=$title, a.type=$type, a.ts=$ts, a.project=$project", p);
    }

    @Override
    public void upsertEdge(Edge e) {
        Map<String, Object> p = new HashMap<>();
        p.put("from", e.from().toString());
        p.put("to", e.to().toString());
        p.put("kind", e.kind().name());
        p.put("weight", e.weight());
        p.put("reason", e.reason());
        run("MATCH (a:Activity {id:$from}), (b:Activity {id:$to}) "
                + "MERGE (a)-[r:RELATED {kind:$kind}]-(b) SET r.weight=$weight, r.reason=$reason", p);
    }

    @Override
    public List<Edge> edgesOf(UUID nodeId, int limit) {
        return read("MATCH (a:Activity {id:$id})-[r:RELATED]-(b:Activity) "
                        + "RETURN a.id AS fromId, b.id AS toId, r.kind AS kind, r.weight AS weight, r.reason AS reason "
                        + "ORDER BY r.weight DESC LIMIT $limit",
                Map.of("id", nodeId.toString(), "limit", limit), Neo4jGraphStore::toEdge);
    }

    @Override
    public List<Node> nodesOf(UUID userId, Instant from, Instant to) {
        return read("MATCH (a:Activity {userId:$uid}) WHERE a.ts >= $from AND a.ts <= $to "
                        + "RETURN a.id AS id, a.title AS title, a.type AS type, a.ts AS ts, a.project AS project ORDER BY a.ts",
                Map.of("uid", userId.toString(), "from", from.toEpochMilli(), "to", to.toEpochMilli()),
                r -> new Node(UUID.fromString(r.get("id").asString()), r.get("title").asString(""),
                        ActivityType.valueOf(r.get("type").asString()), Instant.ofEpochMilli(r.get("ts").asLong()),
                        r.get("project").asString(null)));
    }

    @Override
    public List<Edge> edgesOf(UUID userId, Instant from, Instant to, int limit) {
        return read("MATCH (a:Activity {userId:$uid})-[r:RELATED]-(b:Activity) "
                        + "WHERE a.id < b.id AND a.ts >= $from AND a.ts <= $to AND b.ts >= $from AND b.ts <= $to "
                        + "RETURN a.id AS fromId, b.id AS toId, r.kind AS kind, r.weight AS weight, r.reason AS reason "
                        + "ORDER BY r.weight DESC LIMIT $limit",
                Map.of("uid", userId.toString(), "from", from.toEpochMilli(), "to", to.toEpochMilli(), "limit", limit),
                Neo4jGraphStore::toEdge);
    }

    @Override
    public List<UUID> shortestPath(UUID from, UUID to) {
        List<List<UUID>> rows = read("MATCH (a:Activity {id:$a}), (b:Activity {id:$b}), "
                        + "p = shortestPath((a)-[:RELATED*..6]-(b)) RETURN [n IN nodes(p) | n.id] AS ids",
                Map.of("a", from.toString(), "b", to.toString()),
                r -> r.get("ids").asList(v -> UUID.fromString(v.asString())));
        return rows.isEmpty() ? List.of() : rows.get(0);
    }

    @Override
    public void deleteNode(UUID nodeId) {
        run("MATCH (a:Activity {id:$id}) DETACH DELETE a", Map.of("id", nodeId.toString()));
    }

    private void run(String cypher, Map<String, Object> params) {
        try (Session s = driver.session()) {
            s.run(cypher, params).consume();
        }
    }

    private <T> List<T> read(String cypher, Map<String, Object> params, java.util.function.Function<Record, T> mapper) {
        try (Session s = driver.session()) {
            return s.run(cypher, params).list(mapper::apply);
        }
    }

    private static Edge toEdge(Record r) {
        return new Edge(UUID.fromString(r.get("fromId").asString()), UUID.fromString(r.get("toId").asString()),
                RelationKind.valueOf(r.get("kind").asString()), r.get("weight").asDouble(), r.get("reason").asString(""));
    }
}
