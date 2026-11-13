package com.digitaltimeline.integration;
import com.digitaltimeline.activity.ImportedActivity;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
/** Imports message METADATA only (subject, sender, labels, time). Message bodies are never requested or stored. */
@Component
public class GmailConnector implements ActivityConnector {
  private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me";
  private final GoogleTokenService tokens; private final GoogleApi api;
  public GmailConnector(GoogleTokenService tokens, GoogleApi api) { this.tokens = tokens; this.api = api; }
  @Override public Provider provider() { return Provider.GMAIL; }
  @Override
  public List<ImportedActivity> fetchNewActivities(UUID userId, String cursor) {
    String token = tokens.accessToken(userId, Provider.GMAIL);
    long after = (cursor != null ? Instant.parse(cursor) : Instant.now().minus(30, ChronoUnit.DAYS)).getEpochSecond();
    List<ImportedActivity> out = new ArrayList<>();
    for (JsonNode ref : api.get(token, BASE + "/messages?maxResults=100&q=after:" + after).path("messages")) { // capped at 100 per sync
      String id = ref.path("id").asText();
      JsonNode m = api.get(token, BASE + "/messages/" + id + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From");
      Map<String, String> headers = new HashMap<>();
      m.path("payload").path("headers").forEach(h -> headers.put(h.path("name").asText().toLowerCase(), h.path("value").asText()));
      Set<String> labels = new HashSet<>(); m.path("labelIds").forEach(l -> labels.add(l.asText()));
      String subject = headers.getOrDefault("subject", "").isBlank() ? "(no subject)" : headers.get("subject");
      String desc = "From: " + headers.getOrDefault("from", "unknown") + (labels.contains("UNREAD") ? " · unread" : " · read");
      out.add(new ImportedActivity(Provider.GMAIL, "msg:" + id, "EMAIL", subject, desc,
          "https://mail.google.com/mail/u/0/#all/" + id, Instant.ofEpochMilli(m.path("internalDate").asLong())));
    }
    return out;
  }
}
