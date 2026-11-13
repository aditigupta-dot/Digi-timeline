package com.digitaltimeline.notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class NotificationRepository {
  public record Dto(UUID id, String message, boolean read, Instant createdAt) {}
  private final JdbcTemplate jdbc;
  public NotificationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public void add(UUID userId, String message) { jdbc.update("INSERT INTO notifications(user_id,message) VALUES (?,?)", userId, message); }
  public List<Dto> recent(UUID userId) {
    return jdbc.query("SELECT id,message,\"read\",created_at FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 30",
        (rs, i) -> new Dto(rs.getObject(1, UUID.class), rs.getString(2), rs.getBoolean(3), rs.getTimestamp(4).toInstant()), userId);
  }
  public int unread(UUID userId) { Integer n = jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id=? AND NOT \"read\"", Integer.class, userId); return n == null ? 0 : n; }
  public void markAllRead(UUID userId) { jdbc.update("UPDATE notifications SET \"read\"=true WHERE user_id=? AND NOT \"read\"", userId); }
}
