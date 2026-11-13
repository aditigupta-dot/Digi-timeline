package com.digitaltimeline.activity;
import com.digitaltimeline.exception.ResourceNotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class ActivityRepository {
  public record ActivityDto(UUID id, String source, String type, String title, String description,
                            String sourceUrl, Instant occurredAt) {}
  private static final RowMapper<ActivityDto> MAP = (rs, i) -> new ActivityDto(rs.getObject("id", UUID.class),
      rs.getString("source"), rs.getString("activity_type"), rs.getString("title"), rs.getString("description"),
      rs.getString("source_url"), rs.getTimestamp("occurred_at").toInstant());
  private final JdbcTemplate jdbc;
  public ActivityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  /** Batch insert; the UNIQUE(user_id,source,external_id) constraint silently skips duplicates. Returns new rows. */
  @Transactional
  public int saveAll(UUID userId, List<ImportedActivity> items) {
    int[][] r = jdbc.batchUpdate("""
        INSERT INTO activities(user_id,source,external_id,activity_type,title,description,source_url,occurred_at)
        VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (user_id,source,external_id) DO NOTHING""", items, 100,
        (ps, a) -> { ps.setObject(1, userId); ps.setString(2, a.source().name()); ps.setString(3, a.externalId());
          ps.setString(4, a.type()); ps.setString(5, a.title()); ps.setString(6, a.description());
          ps.setString(7, a.sourceUrl()); ps.setTimestamp(8, Timestamp.from(a.occurredAt())); });
    return (int) Arrays.stream(r).flatMapToInt(Arrays::stream).filter(x -> x > 0 || x == java.sql.Statement.SUCCESS_NO_INFO).count();
  }
  /** Keyset pagination: pass the last item's occurredAt as `before` to get the next page. */
  public List<ActivityDto> search(UUID userId, String source, String q, Instant before, int limit) {
    return jdbc.query("""
        SELECT * FROM activities WHERE user_id=? AND (?::text IS NULL OR source=?)
        AND (?::text IS NULL OR search_vec @@ plainto_tsquery('english', ?))
        AND (?::timestamptz IS NULL OR occurred_at < ?) ORDER BY occurred_at DESC LIMIT ?""", MAP, userId, source,
        source, q, q, before == null ? null : Timestamp.from(before), before == null ? null : Timestamp.from(before),
        Math.min(limit, 100));
  }
  public ActivityDto get(UUID userId, UUID id) {
    var rows = jdbc.query("SELECT * FROM activities WHERE id=? AND user_id=?", MAP, id, userId);
    if (rows.isEmpty()) throw new ResourceNotFoundException("Activity");
    return rows.get(0);
  }
  public List<ActivityDto> recentForExport(UUID userId, int limit) {
    return jdbc.query("SELECT * FROM activities WHERE user_id=? ORDER BY occurred_at DESC LIMIT ?", MAP, userId, limit);
  }
  public List<ActivityDto> since(UUID userId, Instant since, int limit) {
    return jdbc.query("SELECT * FROM activities WHERE user_id=? AND occurred_at >= ? ORDER BY occurred_at DESC LIMIT ?", MAP, userId, Timestamp.from(since), limit);
  }
}
