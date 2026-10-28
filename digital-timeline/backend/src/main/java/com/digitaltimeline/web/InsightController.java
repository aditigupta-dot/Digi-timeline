package com.digitaltimeline.web;

import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.dto.GraphDtos.Cluster;
import com.digitaltimeline.dto.GraphDtos.GraphView;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.insight.ClusterService;
import com.digitaltimeline.insight.DigestService;
import com.digitaltimeline.insight.ForgottenWork;
import com.digitaltimeline.insight.ForgottenWorkDetector;
import com.digitaltimeline.insight.InsightService;
import com.digitaltimeline.insight.InsightsReport;
import com.digitaltimeline.repository.AnalyticsJdbcDao;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InsightController {

    private final InsightService insights;
    private final ForgottenWorkDetector forgotten;
    private final ClusterService clusters;
    private final DigestService digest;
    private final GraphService graph;
    private final AnalyticsJdbcDao jdbc;
    private final Clock clock;

    public InsightController(InsightService insights, ForgottenWorkDetector forgotten, ClusterService clusters,
                             DigestService digest, GraphService graph, AnalyticsJdbcDao jdbc, Clock clock) {
        this.insights = insights;
        this.forgotten = forgotten;
        this.clusters = clusters;
        this.digest = digest;
        this.graph = graph;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @GetMapping("/insights")
    public Map<String, Object> insights(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "30") int days,
                                        @RequestParam(defaultValue = "true") boolean narrative,
                                        @RequestParam(required = false) String tz) {
        UUID userId = CurrentUser.id(jwt);
        InsightsReport report = insights.report(userId, Math.max(1, Math.min(days, 365)), CurrentUser.zone(tz));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("report", report);
        out.put("narrative", narrative ? insights.narrative(userId, report) : null);
        return out;
    }

    @GetMapping("/insights/forgotten")
    public List<ForgottenWork> forgotten(@AuthenticationPrincipal Jwt jwt) {
        return forgotten.detect(CurrentUser.id(jwt), 6);
    }

    @GetMapping("/insights/heatmap")
    public List<AnalyticsJdbcDao.HeatCell> heatmap(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(defaultValue = "90") int days) throws SQLException {
        return jdbc.heatmap(CurrentUser.id(jwt), clock.instant().minus(Math.max(1, Math.min(days, 365)), ChronoUnit.DAYS));
    }

    @GetMapping("/graph")
    public GraphView graph(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "30") int days) {
        Instant now = clock.instant();
        return graph.view(CurrentUser.id(jwt), now.minus(Math.max(1, Math.min(days, 365)), ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS), 600);
    }

    @GetMapping("/graph/path")
    public List<UUID> path(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID from, @RequestParam UUID to) {
        return graph.path(from, to);
    }

    @GetMapping("/clusters")
    public List<Cluster> clusters(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "30") int days) {
        return clusters.threads(CurrentUser.id(jwt), Math.max(1, Math.min(days, 365)), 0.45);
    }

    @PostMapping("/digest")
    public Map<String, Object> digest(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> out = new LinkedHashMap<>();
        digest.createWeeklyDigest(CurrentUser.id(jwt)).ifPresentOrElse(
                a -> out.put("created", (ActivityResponse) a),
                () -> out.put("message", "This week's digest already exists"));
        return out;
    }
}
