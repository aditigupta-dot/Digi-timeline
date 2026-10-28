package com.digitaltimeline.collector;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.service.ActivityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** ADAPTER: converts GitHub's public events JSON into our own ActivityRequest model. */
@Component
public class GitHubCollector extends ActivityCollector {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$");

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String token;

    public GitHubCollector(ActivityService activities, HttpClient http, ObjectMapper mapper,
                           @Value("${dt.collect.github-token:}") String token) {
        super(activities);
        this.http = http;
        this.mapper = mapper;
        this.token = token;
    }

    @Override
    public String source() {
        return "github";
    }

    @Override
    public boolean canAutoRun(User user) {
        return user.getGithubUsername() != null && !user.getGithubUsername().isBlank();
    }

    private String username(User user, Map<String, String> params) {
        String name = params.getOrDefault("username", user.getGithubUsername());
        return name == null ? "" : name.trim();
    }

    @Override
    protected void validate(User user, Map<String, String> params) throws IngestionException {
        String name = username(user, params);
        if (name.isEmpty()) throw new IngestionException(source(), "Set your GitHub username first");
        if (!USERNAME.matcher(name).matches()) throw new IngestionException(source(), "Invalid GitHub username");
    }

    @Override
    protected List<CollectedItem> fetch(User user, Map<String, String> params) throws IngestionException {
        String name = username(user, params);
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder(
                            URI.create("https://api.github.com/users/" + name + "/events/public?per_page=100"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "DigitalTimeline");
            if (!token.isBlank()) req.header("Authorization", "Bearer " + token);
            HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404) throw new IngestionException(source(), "GitHub user not found: " + name);
            if (res.statusCode() == 403 || res.statusCode() == 429) {
                throw new IngestionException(source(), "GitHub rate limit reached. Set GITHUB_TOKEN or retry later");
            }
            if (res.statusCode() / 100 != 2) throw new IngestionException(source(), "GitHub returned HTTP " + res.statusCode());

            List<CollectedItem> items = new ArrayList<>();
            for (JsonNode event : mapper.readTree(res.body())) {
                CollectedItem item = map(event);
                if (item != null) items.add(item);
            }
            return items;
        } catch (IOException e) {
            throw new IngestionException(source(), "Could not reach GitHub: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IngestionException(source(), "Interrupted", e);
        }
    }

    CollectedItem map(JsonNode ev) {
        String type = ev.path("type").asText();
        if (type.equals("WatchEvent") || type.equals("MemberEvent")) return null;
        String repo = ev.path("repo").path("name").asText("unknown/repo");
        JsonNode p = ev.path("payload");
        String title;
        String content = null;
        String sha = null;
        String url = "https://github.com/" + repo;

        switch (type) {
            case "PushEvent" -> {
                int n = Math.max(p.path("size").asInt(0), p.path("commits").size());
                String branch = p.path("ref").asText("").replace("refs/heads/", "");
                title = "Pushed " + (n == 0 ? "commits" : n + (n == 1 ? " commit" : " commits")) + " to " + repo
                        + (branch.isEmpty() ? "" : " (" + branch + ")");
                StringBuilder msgs = new StringBuilder();
                p.path("commits").forEach(c -> msgs.append("- ").append(c.path("message").asText().split("\n")[0]).append('\n'));
                content = msgs.length() == 0 ? null : msgs.toString().trim();
                sha = p.path("head").asText(null);
            }
            case "PullRequestEvent" -> {
                JsonNode pr = p.path("pull_request");
                title = "Pull request " + p.path("action").asText() + ": " + pr.path("title").asText();
                content = pr.path("body").asText(null);
                url = pr.path("html_url").asText(url);
            }
            case "IssuesEvent" -> {
                JsonNode issue = p.path("issue");
                title = "Issue " + p.path("action").asText() + ": " + issue.path("title").asText();
                content = issue.path("body").asText(null);
                url = issue.path("html_url").asText(url);
            }
            case "IssueCommentEvent" -> {
                title = "Commented on: " + p.path("issue").path("title").asText();
                content = p.path("comment").path("body").asText(null);
                url = p.path("comment").path("html_url").asText(url);
            }
            case "CreateEvent" -> title = "Created " + p.path("ref_type").asText("ref") + " "
                    + p.path("ref").asText("") + " in " + repo;
            case "ReleaseEvent" -> {
                title = "Released " + p.path("release").path("name").asText(p.path("release").path("tag_name").asText()) + " in " + repo;
                url = p.path("release").path("html_url").asText(url);
            }
            default -> title = type.replace("Event", "") + " in " + repo;
        }
        if (content != null && content.length() > 3000) content = content.substring(0, 3000);

        String shortRepo = repo.contains("/") ? repo.substring(repo.indexOf('/') + 1) : repo;
        Set<String> tags = new LinkedHashSet<>(List.of("github", shortRepo.toLowerCase()));
        Map<String, String> attrs = new HashMap<>();
        attrs.put("repo", repo);
        attrs.put("eventType", type);
        if (sha != null) attrs.put("sha", sha);
        attrs.put("url", url);
        Instant at = Instant.parse(ev.path("created_at").asText());
        return new CollectedItem(ev.path("id").asText(),
                new ActivityRequest(ActivityType.GITHUB, title, content, at, shortRepo, tags, attrs));
    }
}
