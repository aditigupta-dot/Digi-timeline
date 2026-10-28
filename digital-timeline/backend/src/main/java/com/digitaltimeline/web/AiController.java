package com.digitaltimeline.web;

import com.digitaltimeline.ai.EmbeddingProvider;
import com.digitaltimeline.ai.LlmClient;
import com.digitaltimeline.dto.AiDtos.AskRequest;
import com.digitaltimeline.dto.AiDtos.AskResponse;
import com.digitaltimeline.dto.AiDtos.SearchHit;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.service.ActivityService;
import com.digitaltimeline.service.RagService;
import com.digitaltimeline.service.SearchIndex;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiController {

    private final RagService rag;
    private final ActivityService activities;
    private final LlmClient llm;
    private final EmbeddingProvider embeddings;
    private final GraphService graph;
    private final SearchIndex index;

    public AiController(RagService rag, ActivityService activities, LlmClient llm, EmbeddingProvider embeddings,
                        GraphService graph, SearchIndex index) {
        this.rag = rag;
        this.activities = activities;
        this.llm = llm;
        this.embeddings = embeddings;
        this.graph = graph;
        this.index = index;
    }

    @PostMapping("/ask")
    public AskResponse ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AskRequest r) {
        return rag.ask(CurrentUser.id(jwt), r.question(), CurrentUser.zone(r.tz()));
    }

    @GetMapping("/search")
    public List<SearchHit> search(@AuthenticationPrincipal Jwt jwt, @RequestParam String q,
                                  @RequestParam(defaultValue = "12") int limit) {
        return rag.search(CurrentUser.id(jwt), q, Math.max(1, Math.min(limit, 50)));
    }

    @PostMapping("/reindex")
    public Map<String, Object> reindex(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("reindexed", activities.reindex(CurrentUser.id(jwt)));
    }

    /** Public: tells the UI which AI mode is active (shown as a badge). */
    @GetMapping("/system")
    public Map<String, Object> system() {
        return Map.of("llm", llm.name(), "generative", llm.isGenerative(), "embeddings", embeddings.name(),
                "graph", graph.backend(), "indexedActivities", index.size());
    }
}
