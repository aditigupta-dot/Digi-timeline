package com.digitaltimeline.user;
import com.digitaltimeline.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class UserRepository {
  public record Profile(String email, String displayName, boolean autoSync, boolean aiExternalDefault, Instant createdAt) {}
  private final JdbcTemplate jdbc;
  public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Profile get(UUID id) {
    var r = jdbc.query("SELECT email,display_name,auto_sync,ai_external_default,created_at FROM users WHERE id=?",
        (rs, i) -> new Profile(rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getBoolean(4), rs.getTimestamp(5).toInstant()), id);
    if (r.isEmpty()) throw new ResourceNotFoundException("User");
    return r.get(0);
  }
  /** null fields keep their current value */
  public void update(UUID id, String displayName, Boolean autoSync, Boolean aiExternalDefault) {
    jdbc.update("UPDATE users SET display_name=COALESCE(?,display_name), auto_sync=COALESCE(?,auto_sync), ai_external_default=COALESCE(?,ai_external_default) WHERE id=?",
        displayName, autoSync, aiExternalDefault, id);
  }
  public String passwordHash(UUID id) { return jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, id); }
  public void setPasswordHash(UUID id, String hash) { jdbc.update("UPDATE users SET password_hash=? WHERE id=?", hash, id); }
  public void revokeAllRefresh(UUID id) { jdbc.update("UPDATE refresh_tokens SET revoked=true WHERE user_id=?", id); }
  /** Foreign keys cascade: integrations, tokens, activities, tasks, projects, notifications and AI chats all go with the user. */
  public void delete(UUID id) { jdbc.update("DELETE FROM users WHERE id=?", id); }
}
