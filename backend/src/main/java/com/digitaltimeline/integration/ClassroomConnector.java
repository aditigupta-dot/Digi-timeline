package com.digitaltimeline.integration;
import com.digitaltimeline.activity.ImportedActivity;
import com.digitaltimeline.exception.SyncException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
/** Account-dependent. If Google shows this account no active courses (common for personal accounts), we say so instead of inventing data. */
@Component
public class ClassroomConnector implements ActivityConnector {
  private static final String BASE = "https://classroom.googleapis.com/v1";
  private final GoogleTokenService tokens; private final GoogleApi api;
  public ClassroomConnector(GoogleTokenService tokens, GoogleApi api) { this.tokens = tokens; this.api = api; }
  @Override public Provider provider() { return Provider.CLASSROOM; }
  @Override
  public List<ImportedActivity> fetchNewActivities(UUID userId, String cursor) {
    String token = tokens.accessToken(userId, Provider.CLASSROOM);
    JsonNode courses = api.get(token, BASE + "/courses?studentId=me&courseStates=ACTIVE&pageSize=10").path("courses");
    if (!courses.isArray() || courses.isEmpty())
      throw new SyncException("No active Classroom courses are visible to this account. Classroom data is only available to accounts enrolled in courses (often school or workspace accounts).");
    List<ImportedActivity> out = new ArrayList<>();
    for (JsonNode c : courses) {
      for (JsonNode w : api.get(token, BASE + "/courses/" + c.path("id").asText() + "/courseWork?pageSize=30&orderBy=updateTime%20desc").path("courseWork")) {
        JsonNode d = w.path("dueDate"); Instant at;
        if (d.has("year")) { var date = LocalDate.of(d.path("year").asInt(), d.path("month").asInt(), d.path("day").asInt());
          at = date.atTime(w.path("dueTime").path("hours").asInt(23), w.path("dueTime").path("minutes").asInt(59)).toInstant(ZoneOffset.UTC); }
        else at = Instant.parse(w.path("updateTime").asText());
        out.add(new ImportedActivity(Provider.CLASSROOM, "work:" + w.path("id").asText(), "ASSIGNMENT", w.path("title").asText("(untitled)"),
            c.path("name").asText(null), w.path("alternateLink").asText(null), at));
      }
    }
    return out;
  }
}
