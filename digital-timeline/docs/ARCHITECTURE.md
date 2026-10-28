# Architecture

## 1. System context
```mermaid
flowchart LR
  U[User] --> UI[React UI]
  UI -- REST + JWT --> API[Spring Boot API]
  UI -. SSE live updates .-> API
  API --> PG[(PostgreSQL)]
  API --> N4[(Neo4j knowledge graph)]
  API --> LLM{{LLM: Claude / Ollama / offline}}
  GH[GitHub public events] --> API
  FS[Local folders] --> API
  ICS[Calendar .ics] --> API
```

## 2. Layered components
```mermaid
flowchart TB
  subgraph web[web + security]
    C[Controllers] --- SEC[JWT filter + RateLimitFilter]
    SSE[TimelineStreamService]
  end
  subgraph services[service layer]
    AS[ActivityService] --- RAG[RagService]
    INS[InsightService] --- CL[ClusterService]
    DIG[DigestService] --- FW[ForgottenWorkDetector]
  end
  subgraph ingest[ingestion]
    COL[Collectors: GitHub / Folder / ICS] --> AS
    AS --> PIPE[IngestionPipeline: tags -> embedding -> importance]
  end
  subgraph ai[ai]
    EMB[EmbeddingProvider] --- LLMC[LlmClient]
    PII[PiiRedactor]
  end
  subgraph data[data]
    JPA[JPA repositories] --- JDBC[AnalyticsJdbcDao]
    IDX[SearchIndex in memory] --- G[GraphService -> GraphStore]
  end
  C --> services
  PIPE --> EMB
  RAG --> IDX
  RAG --> G
  RAG --> PII --> LLMC
  AS --> JPA
  AS -- ActivityCreatedEvent --> LINK[LinkingService async]
  LINK --> G
  AS -- event --> SSE
```

## 3. What happens when an activity is saved
```mermaid
sequenceDiagram
  participant UI
  participant API as ActivityController
  participant S as ActivityService
  participant P as IngestionPipeline
  participant DB as PostgreSQL
  participant X as SearchIndex
  participant L as LinkingService (virtual thread)
  participant G as Neo4j
  UI->>API: POST /api/activities
  API->>S: create()
  S->>P: tags, embedding, importance
  S->>DB: save (JPA)
  S->>X: index vector + tokens
  S-->>UI: 201 Created (instant)
  S-)L: ActivityCreatedEvent
  L->>X: nearest neighbours
  L->>L: RelationScorer (weight + reason)
  L->>G: MERGE nodes and RELATED edges
  L-)UI: SSE "linked"
```

## 4. How a question is answered (GraphRAG)
```mermaid
sequenceDiagram
  participant UI
  participant R as RagService
  participant T as TimeRangeParser
  participant X as SearchIndex
  participant G as Graph
  participant P as PiiRedactor
  participant M as LLM
  UI->>R: "what did I do last week on the thesis?"
  R->>T: detect time window
  R->>X: hybrid search (70% semantic, 30% keyword), filtered by window
  R->>G: expand top hits with linked activities
  R->>P: redact personal data
  R->>M: grounded prompt with [A1]..[An]
  M-->>R: answer with citations
  R-->>UI: answer + evidence list (or extractive answer when offline)
```

## 5. Data model
```mermaid
erDiagram
  USERS ||--o{ ACTIVITY : owns
  ACTIVITY ||--o{ ACTIVITY_TAGS : has
  ACTIVITY {
    uuid id PK
    uuid user_id FK
    string kind "TASK NOTE DOCUMENT GITHUB EVENT"
    string title
    string content
    timestamp occurred_at
    string project
    string source
    string external_id "de-duplication"
    bytea embedding
    double importance
  }
```
Graph in Neo4j: `(:Activity {id, userId, title, type, ts, project})-[:RELATED {kind, weight, reason}]-(:Activity)`.
`kind` is one of `SIMILAR_TO`, `SHARES_TAGS`, `SAME_PROJECT`, `FOLLOWS_UP`.

## 6. Deployment
```mermaid
flowchart LR
  Browser --> N[nginx :3000 serves React, proxies /api]
  N --> B[Spring Boot :8080]
  B --> P[(Postgres)]
  B --> G[(Neo4j)]
  B -.optional.-> O[Ollama]
```

## 7. Key design decisions
| Decision | Why |
|---|---|
| Single-table inheritance for activities | One timeline query, no joins; subtype columns are nullable. |
| Official Neo4j driver, not Spring Data Neo4j | Avoids two competing transaction managers next to JPA. |
| In-memory hybrid index first | Instant, dependency-free. For millions of rows replace `SearchIndex` with pgvector (HNSW) behind the same method signatures. |
| Graph writes are async and fault-tolerant | Saving never waits for or fails because of the graph; `GraphService` swallows and logs outages. |
| Offline mode is a first-class citizen | Demos never die on a missing key or bad Wi-Fi. |
| Embeddings stored as bytes | 4 bytes per dimension, portable across PostgreSQL and H2. |
| Activity text is untrusted | Prompts instruct the model to treat it as data; PII is stripped before leaving the machine. |

## 8. Security
BCrypt (cost 12) · stateless JWT HS256 with enforced 32-byte secret · every query scoped by `user_id` (no cross-user reads) · token bucket per user, with a stricter bucket on AI endpoints · folder collector restricted to an allow-list with `toRealPath()` (no traversal, symlinks not followed) · GitHub usernames validated by regex before building URLs · `ObjectInputFilter` on deserialization · CORS allow-list · only the SSE endpoint accepts `?access_token=`.

## 9. Scaling roadmap
pgvector + HNSW index · Flyway migrations · Redis-backed rate limits · OAuth app for private GitHub repos · Slack/Notion/Gmail collectors by adding one `ActivityCollector` subclass each · Kafka between ingestion and linking.
