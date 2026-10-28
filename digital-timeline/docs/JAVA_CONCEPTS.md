# Where every Java concept is used

| Concept | Where to point in the code |
|---|---|
| **OOP: classes and objects** | `domain/*Activity`, `User`, all services |
| **Inheritance** | `Activity` -> `TaskActivity`, `NoteActivity`, `DocumentActivity`, `GitHubActivity`, `EventActivity`; `DigitalTimelineException` -> 5 subclasses |
| **Polymorphism** | `getType()` / `describe()` overridden per subclass; `LlmClient.complete()` behaves differently per provider; `ActivityResponse.from` uses pattern matching on subtype |
| **Abstraction** | `abstract class Activity`, `abstract class ActivityCollector` |
| **Encapsulation** | Private fields + getters/setters in entities; `TokenBucket` hides its refill logic; `SearchIndex` hides its lock |
| **Interfaces** | `LlmClient`, `EmbeddingProvider`, `GraphStore`, `IngestionStage`, `RelationScorer` contract via records |
| **Abstract classes** | `Activity`, `ActivityCollector` (template method) |
| **Collections** | `ArrayList` (results), `HashMap` (index, counts), `HashSet`/`LinkedHashSet` (tags, tokens), `TreeMap` (days sorted), `TreeSet` (streaks), `PriorityQueue` (`TopK`), `LinkedHashMap` access-order (`LruCache`), `ArrayDeque` (BFS shortest path), `EnumMap` (counts by type), `ConcurrentHashMap`, `CopyOnWriteArrayList` |
| **Generics** | `TopK<T>`, `LruCache<K,V>`, `UnionFind<T>`, `Result<T>`, bounded wildcards in `Result.map`, generic `CompletableFuture<CollectionResult>` |
| **Exception handling** | Custom unchecked hierarchy with HTTP status (`exception/`), CHECKED `IngestionException`, try-with-resources everywhere, `GlobalExceptionHandler`, graceful degradation in `RagService` and `GraphService` |
| **File handling** | `FolderCollector` (java.nio.file walk, size/extension/allow-list checks), `SnapshotService` (gzip streams), `.ics` import, JSON export |
| **Multithreading / concurrency** | virtual threads (`AsyncConfig`), `@Async` linking, `CompletableFuture` + `orTimeout` in `CollectorService`, `ReentrantReadWriteLock` in `SearchIndex`, `synchronized` `TokenBucket` and `LruCache`, `ConcurrentHashMap` limiter registry, SSE emitters list |
| **Lambdas and Streams** | `InsightService` (groupingBy, counting, TreeMap collectors, max/sorted/limit), `ActivityService.list` filters, `LinkingService` pipeline, `Comparator.comparingDouble` chains, method references |
| **JDBC** | `AnalyticsJdbcDao`: `PreparedStatement`, `ResultSet`, batch update inside a manual transaction with commit/rollback |
| **Serialization** | `SnapshotItem`/`TimelineSnapshot`: `Serializable`, `serialVersionUID`, `transient` embeddings, validating `readObject`, `ObjectInputFilter` allow-list |
| **Records, sealed types, pattern matching (Java 17-21)** | DTO records, `sealed interface Result`, `switch` pattern matching, text blocks in prompts |
| **Annotations / reflection (via frameworks)** | JPA, validation, `@ConfigurationProperties`, `@EventListener`, `@Async`, `@Scheduled` |

## Design patterns
| Pattern | Class |
|---|---|
| Strategy | `LlmClient`, `EmbeddingProvider`, `GraphStore` |
| Factory | `ActivityFactory`, `AiConfig` bean selection |
| Template Method | `ActivityCollector.collect()` |
| Chain of Responsibility | `IngestionPipeline` stages, `PiiRedactor` rule chain |
| Observer | `ActivityCreatedEvent` / `ActivityLinkedEvent` listeners |
| Decorator | `CachingEmbeddingProvider` |
| Adapter | `GitHubCollector.map` (GitHub JSON -> our model) |
| Facade | `GraphService` |
| Null Object | `OfflineLlmClient` |
| Singleton | Spring beans |
| Builder | `JwtClaimsSet.builder()`, `HttpRequest.newBuilder()` usage |
| Proxy | `@Transactional`, `@Async` (Spring AOP) |

## Algorithms
Top-K with a bounded min-heap · cosine similarity · cosine centroid · hashing-trick embeddings · Jaccard similarity · union-find with path compression and union by rank · BFS shortest path · token bucket · LRU eviction · FNV-1a hashing.
