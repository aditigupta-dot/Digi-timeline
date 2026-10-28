# DigitalTimeline: AI-Powered Digital Activity Intelligence

> DigitalTimeline converts scattered digital activities into an intelligent, searchable timeline using Java, modern backend technologies, knowledge graphs and AI.

Tasks, notes, documents, GitHub activity and calendar events flow into **one chronological timeline**. The system embeds each item, **links related ones in a Neo4j knowledge graph with a human-readable reason**, and lets you **ask questions in plain English** ("what did I do last week on the thesis?") with answers that cite their evidence.

## What makes it different

| Feature | What it does |
|---|---|
| **GraphRAG with citations** | Hybrid retrieval (embeddings + keywords) is expanded through the knowledge graph, then the LLM answers using only that evidence and cites it as `[A1]`, `[A2]`. |
| **Explainable links** | Every graph edge carries a reason: "similar content (62%); shared tags: topology; same project". No black-box scores. |
| **Natural time understanding** | "yesterday", "last week", "in March", "last 10 days" become date filters before the AI is called. |
| **Forgotten-work detector** | Finds open tasks you abandoned that are still *semantically close* to what you worked on this week. |
| **Threads of work** | Union-find over strong graph links discovers project threads automatically. |
| **Works with zero setup** | No API key? A hashing-trick embedding model and extractive answers keep every feature working offline. Plug in Claude or Ollama when you want. |
| **Privacy by design** | A chain of PII filters (emails, phones, Aadhaar, card numbers, API keys, GitHub tokens) runs before anything reaches an external LLM. Prompts also treat activity text as data, which blocks prompt injection from commit messages or invites. |
| **AI writes into your timeline** | The weekly digest is saved as a note, so it is searchable history too. |
| **Live updates** | New activities and new graph links stream to the browser over Server-Sent Events. |
| **Safe backup** | Gzip + Java serialization with an `ObjectInputFilter` allow-list, `transient` embeddings and validating `readObject`. |

## New in this version
- **Hero / login page** with animated books, pen, brain, notebook and paper (SVG, respects reduced-motion settings).
- **Dashboard** in the maroon-and-blush design: greeting, search, schedule calendar, upcoming events, projects, progress rings.
- **Reminders**: bell icon with overdue, backlog (open 7+ days), due-soon tasks and events this week; a once-per-session banner; optional desktop alerts.
- **DigiBot** chat widget (bottom-right): answers about your timeline with evidence, plus instant answers for backlog and upcoming events.
- **Knowledge Map** rebuilt to be readable: how-to guide, project colours, click-to-focus, plain-English "because ..." reasons, and Story threads.

## Run it

### Option A: 60 seconds, nothing to install except Java 21 and Node (H2 + in-memory graph + offline AI)
```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev        # open http://localhost:5173
```
Sign in with the pre-filled **demo@digitaltimeline.dev / demo1234**. It already contains five weeks of sample activity.

### Option B: full stack (PostgreSQL + Neo4j + backend + UI)
```bash
cp .env.example .env            # set JWT_SECRET
docker compose up --build       # UI http://localhost:3000, Neo4j Browser http://localhost:7474
```

### Turn on real AI (optional)
```bash
# Claude for written answers
DT_LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... docker compose up -d backend
# Fully local: docker compose --profile local-ai up -d, then DT_LLM_PROVIDER=ollama DT_EMBEDDING_PROVIDER=ollama
```
After switching the embedding provider press **Re-index embeddings** in *Sources & backup*.

## Tech stack
Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Hibernate/JPA · JDBC · PostgreSQL · Neo4j · React 18 + Vite · Recharts · Docker Compose · GitHub Actions

## Project layout
```
backend/src/main/java/com/digitaltimeline/
  core/        pure Java utilities (no Spring): TopK, LruCache, TokenBucket, UnionFind, Result, TimeRangeParser, PiiRedactor, IcsParser
  domain/      JPA entities: abstract Activity + 5 subclasses, User
  ingest/      chain-of-responsibility pipeline (tags -> embedding -> importance)
  collector/   template-method collectors: GitHub, folder, calendar + parallel runner
  ai/          LLM and embedding strategies, decorator cache, offline narrator
  graph/       GraphStore strategy: Neo4j and in-memory, resilient facade
  link/        explainable relationship engine (async, virtual threads)
  service/     ActivityService, SearchIndex (hybrid), RagService, AuthService
  insight/     analytics, forgotten work, clusters, weekly digest
  export/      serialization backup and restore
  security/    JWT, BCrypt, per-user rate limiting
  web/         REST controllers, SSE, global error handler
frontend/src/  React app: timeline, ask, graph, insights, sources
docs/          ARCHITECTURE.md, JAVA_CONCEPTS.md, DEMO_SCRIPT.md, API.md
```

## Documentation
- [Architecture and diagrams](docs/ARCHITECTURE.md)
- [Where every Java concept is used](docs/JAVA_CONCEPTS.md)
- [5-minute demo script and viva questions](docs/DEMO_SCRIPT.md)
- [API reference](docs/API.md)

## Verification status
The pure-Java core (`core/` package) was compiled and exercised with a smoke test, and the React app builds with Vite. The Spring Boot modules were written against Spring Boot 3.3.5 / Java 21 but **could not be compiled in the environment where this was generated** (no Maven access). Run `mvn -q verify` first; if the compiler flags anything, it will be small (an import or a version tweak). The unit tests in `backend/src/test` run with that command.
