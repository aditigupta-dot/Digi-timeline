package com.digitaltimeline.integration;
import com.digitaltimeline.activity.ImportedActivity;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.digitaltimeline.security.TokenCipher;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
/** Imports the user's recent commits, issues and pull requests. `cursor` is an ISO time: only newer items are fetched. */
@Component
public class GithubConnector implements ActivityConnector {
  private final GithubApi api; private final IntegrationRepository integrations; private final TokenCipher cipher;
  public GithubConnector(GithubApi api, IntegrationRepository integrations, TokenCipher cipher) { this.api = api; this.integrations = integrations; this.cipher = cipher; }
  @Override public Provider provider() { return Provider.GITHUB; }
  @Override
  public List<ImportedActivity> fetchNewActivities(UUID userId, String cursor) {
    String token = cipher.decrypt(integrations.accessTokenEnc(userId, Provider.GITHUB)
        .orElseThrow(() -> new UnauthorizedIntegrationException("GitHub is not connected.")));
    String since = cursor != null ? cursor : Instant.now().minus(30, ChronoUnit.DAYS).toString();
    String login = api.get(token, "/user").path("login").asText();
    List<ImportedActivity> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
    for (JsonNode repo : api.get(token, "/user/repos?sort=pushed&per_page=10")) { // 10 most recently pushed repos
      String full = repo.path("full_name").asText();
      for (JsonNode c : api.get(token, "/repos/" + full + "/commits?author=" + URLEncoder.encode(login, StandardCharsets.UTF_8) + "&since=" + since + "&per_page=50")) {
        String sha = c.path("sha").asText();
        if (!seen.add("commit:" + sha)) continue;
        String msg = c.path("commit").path("message").asText("(no message)");
        out.add(new ImportedActivity(Provider.GITHUB, "commit:" + sha, "COMMIT", msg.lines().findFirst().orElse(msg), full,
            c.path("html_url").asText(null), Instant.parse(c.path("commit").path("author").path("date").asText())));
      }
    }
    for (JsonNode i : api.get(token, "/user/issues?filter=all&state=all&since=" + since + "&per_page=50")) { // issues AND pull requests
      boolean pr = i.has("pull_request");
      if (!seen.add("issue:" + i.path("id").asText())) continue;
      out.add(new ImportedActivity(Provider.GITHUB, "issue:" + i.path("id").asText(), pr ? "PULL_REQUEST" : "ISSUE",
          "#" + i.path("number").asInt() + " " + i.path("title").asText(), i.path("repository").path("full_name").asText(null),
          i.path("html_url").asText(null), Instant.parse(i.path("updated_at").asText())));
    }
    out.sort(Comparator.comparing(ImportedActivity::occurredAt).reversed());
    return out;
  }
}
