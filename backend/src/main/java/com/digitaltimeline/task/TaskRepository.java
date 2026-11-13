package com.digitaltimeline.task;
import com.digitaltimeline.exception.ResourceNotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
/** Every query filters by user_id, so one user can never read or change another user's tasks. */
@Repository
public class TaskRepository {
  public record TaskDto(UUID id, UUID projectId, String title, String notes, String status, int priority,
                        Instant dueAt, boolean suggestion) {}
  private static final RowMapper<TaskDto> MAP = (rs, i) -> new TaskDto(rs.getObject("id", UUID.class),
      rs.getObject("project_id", UUID.class), rs.getString("title"), rs.getString("notes"), rs.getString("status"),
      rs.getInt("priority"), rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toInstant(),
      rs.getBoolean("is_suggestion"));
  private final JdbcTemplate jdbc;
  public TaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<TaskDto> list(UUID userId, String status, String q) {
    return jdbc.query("""
        SELECT * FROM tasks WHERE user_id=? AND (?::text IS NULL OR status=?)
        AND (?::text IS NULL OR title ILIKE '%'||?||'%') ORDER BY due_at NULLS LAST, priority""",
        MAP, userId, status, status, q, q);
  }
  public TaskDto create(UUID userId, String title, String notes, int priority, Instant dueAt, UUID projectId) {
    if (projectId != null) { // a task may only point at the user's OWN project
      Integer own = jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=? AND user_id=?", Integer.class, projectId, userId);
      if (own == null || own == 0) throw new ResourceNotFoundException("Project");
    }
    return jdbc.queryForObject("INSERT INTO tasks(user_id,title,notes,priority,due_at,project_id) VALUES (?,?,?,?,?,?) RETURNING *",
        MAP, userId, title, notes, priority, dueAt == null ? null : Timestamp.from(dueAt), projectId);
  }
  /** PATCH semantics: null fields keep their current value. */
  public TaskDto update(UUID userId, UUID id, String title, String notes, String status, Integer priority, Instant dueAt, Boolean confirm) {
    var rows = jdbc.query("""
        UPDATE tasks SET title=COALESCE(?,title), notes=COALESCE(?,notes), status=COALESCE(?,status),
        priority=COALESCE(?,priority), due_at=COALESCE(?,due_at), is_suggestion=CASE WHEN ? THEN false ELSE is_suggestion END
        WHERE id=? AND user_id=? RETURNING *""", MAP, title, notes, status, priority,
        dueAt == null ? null : Timestamp.from(dueAt), Boolean.TRUE.equals(confirm), id, userId);
    if (rows.isEmpty()) throw new ResourceNotFoundException("Task");
    return rows.get(0);
  }
  public void delete(UUID userId, UUID id) {
    if (jdbc.update("DELETE FROM tasks WHERE id=? AND user_id=?", id, userId) == 0) throw new ResourceNotFoundException("Task");
  }
  /** Open tasks due within the next 7 days (or overdue), soonest first. */
  public List<TaskDto> upcoming(UUID userId) {
    return jdbc.query("SELECT * FROM tasks WHERE user_id=? AND status<>'COMPLETED' AND due_at IS NOT NULL AND due_at < now()+interval '7 days' ORDER BY due_at LIMIT 10", MAP, userId);
  }
  public int countOpen(UUID userId) {
    Integer n = jdbc.queryForObject("SELECT count(*) FROM tasks WHERE user_id=? AND status<>'COMPLETED' AND NOT is_suggestion", Integer.class, userId);
    return n == null ? 0 : n;
  }
}
