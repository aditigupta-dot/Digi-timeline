# 5-minute demo for sir

**Pitch (15 s):** "DigitalTimeline converts scattered digital activities into an intelligent, searchable timeline using Java, modern backend technologies, knowledge graphs and AI."

1. **Timeline (45 s).** Sign in as the demo user. Scroll five weeks of activity. Filter by project and type. Point out the live badge.
2. **Add an activity live (30 s).** Add the note "Meeting with supervisor about the ablation #thesis". It appears instantly; click it a few seconds later and show the *connections* the system found, with reasons.
3. **Search by meaning (30 s).** Search "noisy signal classification". Results match even without the exact words.
4. **Ask your timeline (60 s).** Ask "What did I work on last week?" then "How did the thesis results evolve?". Show the evidence list, the time window it detected, and the answer mode. Mention PII redaction.
5. **Knowledge graph (45 s).** Open the graph, hover an edge to read its reason, then show the automatically detected threads. Open Neo4j Browser (`MATCH (a)-[r]-(b) RETURN a,r,b LIMIT 50`).
6. **Insights (45 s).** Focus score, peak hour heatmap (JDBC), and **Forgotten work**.
7. **Java concepts (60 s).** Open `docs/JAVA_CONCEPTS.md` and show 3 files: `Activity` (inheritance), `TopK` + `SearchIndex` (collections, generics, locks), `CollectorService` (concurrency).

## Likely viva questions
- **Why single-table inheritance?** One timeline query without joins; trade-off is nullable subtype columns.
- **How is RAG different from just calling an LLM?** The model only sees retrieved, user-owned evidence and must cite it; that cuts hallucination and makes answers checkable.
- **Why a graph database?** Relationships are the product. Neighbour expansion and shortest paths are one Cypher line, awkward in SQL.
- **What if the LLM is down?** `RagService` catches `AiProviderException` and returns an extractive answer; the mode field says so.
- **How do you stop prompt injection?** Activity text is wrapped as data, the system prompt forbids following it, answers must cite labels, and output is never executed.
- **Thread safety?** `SearchIndex` uses a read/write lock; the limiter uses a `ConcurrentHashMap` of synchronized buckets; linking runs on virtual threads.
- **Why is deserialization dangerous and what did you do?** Untrusted streams can instantiate arbitrary classes. We use an allow-list `ObjectInputFilter`, size limits and a validating `readObject`.
- **How would it scale?** Move vectors to pgvector/HNSW, add Flyway, put Kafka between ingestion and linking.
