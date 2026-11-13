package com.digitaltimeline.export;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.integration.IntegrationRepository;
import com.digitaltimeline.notification.NotificationRepository;
import com.digitaltimeline.project.ProjectRepository;
import com.digitaltimeline.task.TaskRepository;
import com.digitaltimeline.user.UserRepository;
import com.digitaltimeline.util.CsvWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** Data export. The JSON export never includes password hashes or provider tokens (integration rows hold status only). */
@RestController @RequestMapping("/api/export")
public class ExportController {
  private final ActivityRepository activities; private final TaskRepository tasks; private final ProjectRepository projects;
  private final IntegrationRepository integrations; private final NotificationRepository notifications; private final UserRepository users;
  public ExportController(ActivityRepository a, TaskRepository t, ProjectRepository p, IntegrationRepository i, NotificationRepository n, UserRepository u) {
    activities = a; tasks = t; projects = p; integrations = i; notifications = n; users = u;
  }
  @GetMapping("/timeline.csv")
  ResponseEntity<byte[]> timeline(@AuthenticationPrincipal UUID u) {
    StringBuilder sb = new StringBuilder("\uFEFF").append(CsvWriter.row("Date", "Source", "Type", "Title", "Description", "URL"));
    activities.recentForExport(u, 5000).forEach(a -> sb.append(CsvWriter.row(a.occurredAt(), a.source(), a.type(), a.title(), a.description(), a.sourceUrl())));
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"digitaltimeline-export.csv\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(sb.toString().getBytes(StandardCharsets.UTF_8));
  }
  @GetMapping("/all.json")
  ResponseEntity<Map<String, Object>> everything(@AuthenticationPrincipal UUID u) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("exportedAt", Instant.now()); out.put("profile", users.get(u));
    out.put("activities", activities.recentForExport(u, 5000)); // latest 5,000
    out.put("tasks", tasks.list(u, null, null)); out.put("projects", projects.list(u));
    out.put("integrations", integrations.list(u)); out.put("notifications", notifications.recent(u));
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"digitaltimeline-data.json\"").body(out);
  }
}
