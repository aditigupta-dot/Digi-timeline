package com.digitaltimeline.integration;
import com.digitaltimeline.activity.ImportedActivity;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
/** Imports events from the PRIMARY calendar in a window of 7 days back to 60 days ahead (so upcoming deadlines appear). */
@Component
public class CalendarConnector implements ActivityConnector {
  private final GoogleTokenService tokens; private final GoogleApi api;
  public CalendarConnector(GoogleTokenService tokens, GoogleApi api) { this.tokens = tokens; this.api = api; }
  @Override public Provider provider() { return Provider.CALENDAR; }
  private static String enc(Instant i) { return URLEncoder.encode(i.toString(), StandardCharsets.UTF_8); }
  @Override
  public List<ImportedActivity> fetchNewActivities(UUID userId, String cursor) {
    String token = tokens.accessToken(userId, Provider.CALENDAR);
    Instant now = Instant.now();
    JsonNode events = api.get(token, "https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&maxResults=250&timeMin="
        + enc(now.minus(7, ChronoUnit.DAYS)) + "&timeMax=" + enc(now.plus(60, ChronoUnit.DAYS)));
    List<ImportedActivity> out = new ArrayList<>();
    for (JsonNode e : events.path("items")) {
      if ("cancelled".equals(e.path("status").asText())) continue;
      JsonNode s = e.path("start");
      Instant at = s.hasNonNull("dateTime") ? OffsetDateTime.parse(s.get("dateTime").asText()).toInstant()
          : LocalDate.parse(s.path("date").asText()).atStartOfDay(ZoneOffset.UTC).toInstant(); // all-day event
      String d = e.path("description").asText(null);
      out.add(new ImportedActivity(Provider.CALENDAR, "event:" + e.path("id").asText(), "CALENDAR_EVENT", e.path("summary").asText("(untitled event)"),
          d == null ? null : d.substring(0, Math.min(d.length(), 500)), e.path("htmlLink").asText(null), at));
    }
    return out;
  }
}
