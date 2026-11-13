package com.digitaltimeline.dashboard;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.activity.ActivityRepository.ActivityDto;
import com.digitaltimeline.task.TaskRepository;
import com.digitaltimeline.task.TaskRepository.TaskDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** Everything here is read from the database. Providers with no row are shown as DISCONNECTED by the UI. */
@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
  public record IntegrationRow(String provider, String status, String account, Instant lastSyncAt, String lastError) {}
  public record DashboardDto(int openTasks, int activitiesThisWeek, List<TaskDto> upcoming,
                             List<ActivityDto> recent, List<IntegrationRow> integrations) {}
  private final TaskRepository tasks; private final ActivityRepository activities; private final JdbcTemplate jdbc;
  public DashboardController(TaskRepository t, ActivityRepository a, JdbcTemplate j) { tasks = t; activities = a; jdbc = j; }
  @GetMapping("/today")
  DashboardDto today(@AuthenticationPrincipal UUID u) {
    Integer week = jdbc.queryForObject("SELECT count(*) FROM activities WHERE user_id=? AND occurred_at >= ?", Integer.class,
        u, Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS)));
    var integ = jdbc.query("SELECT provider,status,account_label,last_sync_at,last_error FROM integrations WHERE user_id=?",
        (rs, i) -> new IntegrationRow(rs.getString(1), rs.getString(2), rs.getString(3),
            rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant(), rs.getString(5)), u);
    return new DashboardDto(tasks.countOpen(u), week == null ? 0 : week, tasks.upcoming(u), activities.search(u, null, null, null, 6), integ);
  }
}
