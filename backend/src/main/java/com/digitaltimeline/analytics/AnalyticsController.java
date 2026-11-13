package com.digitaltimeline.analytics;
import com.digitaltimeline.project.ProjectRepository;
import com.digitaltimeline.project.ProjectRepository.ProjectDto;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * All numbers come from SQL over the user's own rows. Days with no activity are returned as 0 (generate_series + LEFT JOIN)
 * so charts have no gaps. Note: tasks have no completion timestamp yet, so "completion" is the current share of tasks marked done.
 */
@RestController @RequestMapping("/api/analytics")
public class AnalyticsController {
  public record Point(String label, int count) {}
  public record Weekly(List<Point> daily, List<Point> weekly, List<Point> bySource, int tasksTotal, int tasksDone, List<ProjectDto> projects) {}
  private static final RowMapper<Point> POINT = (rs, i) -> new Point(rs.getString("label"), rs.getInt("n"));
  private final JdbcTemplate jdbc; private final ProjectRepository projects;
  public AnalyticsController(JdbcTemplate jdbc, ProjectRepository projects) { this.jdbc = jdbc; this.projects = projects; }
  @GetMapping("/weekly")
  Weekly weekly(@AuthenticationPrincipal UUID u) {
    var daily = jdbc.query("""
        SELECT to_char(d,'Mon DD') AS label, count(a.id) AS n FROM generate_series(current_date-13, current_date, interval '1 day') d
        LEFT JOIN activities a ON a.user_id=? AND a.occurred_at >= d AND a.occurred_at < d + interval '1 day' GROUP BY d ORDER BY d""", POINT, u);
    var weekly = jdbc.query("""
        SELECT to_char(w,'Mon DD') AS label, count(a.id) AS n FROM generate_series(date_trunc('week',current_date)-interval '7 weeks', date_trunc('week',current_date), interval '1 week') w
        LEFT JOIN activities a ON a.user_id=? AND a.occurred_at >= w AND a.occurred_at < w + interval '1 week' GROUP BY w ORDER BY w""", POINT, u);
    var bySource = jdbc.query("SELECT source AS label, count(*) AS n FROM activities WHERE user_id=? GROUP BY source ORDER BY n DESC", POINT, u);
    int[] t = jdbc.queryForObject("SELECT count(*), count(*) FILTER (WHERE status='COMPLETED') FROM tasks WHERE user_id=? AND NOT is_suggestion",
        (rs, i) -> new int[] {rs.getInt(1), rs.getInt(2)}, u);
    return new Weekly(daily, weekly, bySource, t[0], t[1], projects.list(u));
  }
}
