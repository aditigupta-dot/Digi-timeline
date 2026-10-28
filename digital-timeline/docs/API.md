# REST API (Swagger UI at /swagger-ui.html)

All endpoints except `/api/auth/*` and `/api/system` need `Authorization: Bearer <jwt>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Create account / sign in, returns JWT |
| GET, PUT | `/api/me` | Profile, set GitHub username |
| GET | `/api/timeline?days&types&project&tz` | Activities grouped by local day |
| POST | `/api/activities` | Create (type-specific fields go in `attrs`) |
| GET, PUT, DELETE | `/api/activities/{id}` | Read, update, delete |
| GET | `/api/activities/{id}/related` | Graph neighbours with reasons |
| GET | `/api/search?q=` | Hybrid semantic + keyword search |
| POST | `/api/ask` | `{question, tz}` -> answer, citations, follow-ups |
| GET | `/api/graph?days=` | Nodes and edges |
| GET | `/api/graph/path?from&to` | Shortest connection between two activities |
| GET | `/api/clusters?days=` | Threads of work |
| GET | `/api/insights?days=` | Metrics + AI narrative |
| GET | `/api/insights/forgotten` | Forgotten open tasks |
| GET | `/api/insights/heatmap?days=` | JDBC day x hour counts |
| POST | `/api/digest` | Write weekly digest into the timeline |
| POST | `/api/collect/github`, `/folder`, `/calendar`, `/sync-all` | Import from sources |
| GET | `/api/export/snapshot`, `/api/export/json` | Backups |
| POST | `/api/import/snapshot` | Restore (multipart `file`) |
| POST | `/api/reindex` | Re-embed everything |
| GET | `/api/stream?access_token=` | Server-sent events: `activity`, `linked` |
| GET | `/api/system` | Active LLM, embeddings, graph backend |

Errors use RFC 7807 JSON (`{title, status, detail}`); rate limit breaches return `429` with `Retry-After`.
