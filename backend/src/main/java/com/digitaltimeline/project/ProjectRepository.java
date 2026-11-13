package com.digitaltimeline.project;
import com.digitaltimeline.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
@Repository
public class ProjectRepository {
  public record ProjectDto(UUID id, String name, String description, int totalTasks, int doneTasks) {}
  private static final RowMapper<ProjectDto> MAP = (rs, i) -> new ProjectDto(rs.getObject("id", UUID.class),
      rs.getString("name"), rs.getString("description"), rs.getInt("total"), rs.getInt("done"));
  private static final String SELECT = """
      SELECT p.id,p.name,p.description,count(t.id) AS total,count(t.id) FILTER (WHERE t.status='COMPLETED') AS done
      FROM projects p LEFT JOIN tasks t ON t.project_id=p.id AND t.user_id=p.user_id WHERE p.user_id=? """;
  private final JdbcTemplate jdbc;
  public ProjectRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<ProjectDto> list(UUID userId) { return jdbc.query(SELECT + "GROUP BY p.id ORDER BY p.created_at DESC", MAP, userId); }
  public ProjectDto get(UUID userId, UUID id) {
    var r = jdbc.query(SELECT + "AND p.id=? GROUP BY p.id", MAP, userId, id);
    if (r.isEmpty()) throw new ResourceNotFoundException("Project");
    return r.get(0);
  }
  public ProjectDto create(UUID userId, String name, String description) {
    UUID id = jdbc.queryForObject("INSERT INTO projects(user_id,name,description) VALUES (?,?,?) RETURNING id", UUID.class, userId, name, description);
    return get(userId, id);
  }
}
