package com.digitaltimeline.ai;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class AiRepository {
  private final JdbcTemplate jdbc;
  public AiRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public UUID createConversation(UUID userId) { return jdbc.queryForObject("INSERT INTO ai_conversations(user_id) VALUES (?) RETURNING id", UUID.class, userId); }
  public boolean owns(UUID userId, UUID conversationId) {
    Integer n = jdbc.queryForObject("SELECT count(*) FROM ai_conversations WHERE id=? AND user_id=?", Integer.class, conversationId, userId); return n != null && n > 0;
  }
  public void addMessage(UUID conversationId, String role, String content, List<UUID> activityIds) {
    jdbc.update("INSERT INTO ai_messages(conversation_id,role,content,referenced_activity_ids) VALUES (?,?,?,?)", ps -> {
      ps.setObject(1, conversationId); ps.setString(2, role); ps.setString(3, content);
      ps.setArray(4, activityIds == null ? null : ps.getConnection().createArrayOf("uuid", activityIds.toArray())); });
  }
}
