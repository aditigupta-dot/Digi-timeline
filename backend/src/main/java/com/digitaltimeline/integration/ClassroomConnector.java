package com.digitaltimeline.integration;

import com.digitaltimeline.activity.ImportedActivity;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Imports a student's active Google Classroom courses and their coursework.
 * Empty course lists are valid: personal Google accounts often have no enrolled courses.
 */
@Component
public class ClassroomConnector implements ActivityConnector {
  private static final String BASE = "https://classroom.googleapis.com/v1";
  private static final int PAGE_SIZE = 100;

  private final GoogleTokenService tokens;
  private final GoogleApi api;

  public ClassroomConnector(GoogleTokenService tokens, GoogleApi api) {
    this.tokens = tokens;
    this.api = api;
  }

  @Override
  public Provider provider() {
    return Provider.CLASSROOM;
  }

  @Override
  public List<ImportedActivity> fetchNewActivities(UUID userId, String cursor) {
    String token = tokens.accessToken(userId, Provider.CLASSROOM);
    List<JsonNode> courses = fetchAll(token,
        BASE + "/courses?studentId=me&courseStates=ACTIVE&pageSize=" + PAGE_SIZE,
        "courses");

    // No active courses is a successful, empty sync—not an authorization failure.
    if (courses.isEmpty()) {
      return List.of();
    }

    List<ImportedActivity> activities = new ArrayList<>();
    for (JsonNode course : courses) {
      String courseId = course.path("id").asText("");
      if (courseId.isBlank()) {
        continue;
      }

      String courseworkUrl = BASE + "/courses/" + courseId
          + "/courseWork?pageSize=" + PAGE_SIZE + "&orderBy=updateTime%20desc";
      List<JsonNode> coursework = fetchAll(token, courseworkUrl, "courseWork");

      for (JsonNode work : coursework) {
        String workId = work.path("id").asText("");
        if (workId.isBlank()) {
          continue;
        }

        String title = work.path("title").asText("(untitled assignment)");
        String courseName = course.path("name").asText("Google Classroom course");
        String link = work.path("alternateLink").asText(null);

        activities.add(new ImportedActivity(
            Provider.CLASSROOM,
            "course:" + courseId + ":work:" + workId,
            "ASSIGNMENT",
            title,
            courseName,
            link,
            activityTime(work)
        ));
      }
    }
    return activities;
  }

  /** Reads every page returned by a Google Classroom list endpoint. */
  private List<JsonNode> fetchAll(String token, String firstUrl, String collectionName) {
    List<JsonNode> result = new ArrayList<>();
    String pageToken = null;

    do {
      String url = firstUrl;
      if (pageToken != null && !pageToken.isBlank()) {
        url += "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8);
      }

      JsonNode response = api.get(token, url);
      JsonNode items = response.path(collectionName);
      if (items.isArray()) {
        items.forEach(result::add);
      }
      pageToken = response.path("nextPageToken").asText(null);
    } while (pageToken != null && !pageToken.isBlank());

    return result;
  }

  /** Prefer the assignment deadline; fall back to the last update time when no deadline exists. */
  private Instant activityTime(JsonNode work) {
    JsonNode dueDate = work.path("dueDate");
    if (dueDate.hasNonNull("year") && dueDate.hasNonNull("month") && dueDate.hasNonNull("day")) {
      try {
        LocalDate date = LocalDate.of(
            dueDate.path("year").asInt(),
            dueDate.path("month").asInt(),
            dueDate.path("day").asInt()
        );
        JsonNode dueTime = work.path("dueTime");
        int hour = dueTime.path("hours").asInt(23);
        int minute = dueTime.path("minutes").asInt(59);
        int second = dueTime.path("seconds").asInt(0);
        return date.atTime(hour, minute, second).toInstant(ZoneOffset.UTC);
      } catch (RuntimeException ignored) {
        // A malformed or partial due date should not prevent the rest of the course from syncing.
      }
    }

    String updated = work.path("updateTime").asText("");
    if (!updated.isBlank()) {
      try {
        return Instant.parse(updated);
      } catch (DateTimeParseException ignored) {
        // Fall through to creation time.
      }
    }

    String created = work.path("creationTime").asText("");
    if (!created.isBlank()) {
      try {
        return Instant.parse(created);
      } catch (DateTimeParseException ignored) {
        // Fall through to the current time for an unexpectedly incomplete API record.
      }
    }
    return Instant.now();
  }
}
