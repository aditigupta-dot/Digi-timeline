package com.digitaltimeline.integration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class IntegrationRepository {
  public record Row(String provider, String status, String account, Instant lastSyncAt, String lastError) {}
  private final JdbcTemplate jdbc;
  public IntegrationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<Row> list(UUID userId) {
    return jdbc.query("SELECT provider,status,account_label,last_sync_at,last_error FROM integrations WHERE user_id=?",
        (rs, i) -> new Row(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant(), rs.getString(5)), userId);
  }
  public Optional<UUID> connectedId(UUID userId, Provider p) {
    return jdbc.query("SELECT id FROM integrations WHERE user_id=? AND provider=? AND status<>'DISCONNECTED'", (rs, i) -> rs.getObject(1, UUID.class), userId, p.name()).stream().findFirst();
  }
  public UUID upsertConnected(UUID userId, Provider p, String label) {
    return jdbc.queryForObject("""
        INSERT INTO integrations(user_id,provider,status,account_label) VALUES (?,?, 'CONNECTED',?)
        ON CONFLICT (user_id,provider) DO UPDATE SET status='CONNECTED', account_label=EXCLUDED.account_label, last_error=NULL RETURNING id""",
        UUID.class, userId, p.name(), label);
  }
  public void saveTokens(UUID integrationId, String accessEnc, String refreshEnc, String scopes) {
    jdbc.update("""
        INSERT INTO oauth_tokens(integration_id,access_token_enc,refresh_token_enc,scopes) VALUES (?,?,?,?)
        ON CONFLICT (integration_id) DO UPDATE SET access_token_enc=EXCLUDED.access_token_enc, refresh_token_enc=EXCLUDED.refresh_token_enc, scopes=EXCLUDED.scopes""",
        integrationId, accessEnc, refreshEnc, scopes);
  }
  public Optional<String> accessTokenEnc(UUID userId, Provider p) {
    return jdbc.query("SELECT t.access_token_enc FROM oauth_tokens t JOIN integrations i ON i.id=t.integration_id WHERE i.user_id=? AND i.provider=?",
        (rs, i) -> rs.getString(1), userId, p.name()).stream().findFirst();
  }
  public Optional<String> cursor(UUID integrationId) {
    return jdbc.query("SELECT cursor FROM sync_state WHERE integration_id=?", (rs, i) -> rs.getString(1), integrationId).stream().findFirst();
  }
  public UUID startHistory(UUID integrationId) {
    return jdbc.queryForObject("INSERT INTO sync_history(integration_id) VALUES (?) RETURNING id", UUID.class, integrationId);
  }
  public void markSuccess(UUID integrationId, UUID historyId, String newCursor, int imported) {
    jdbc.update("UPDATE integrations SET status='CONNECTED', last_sync_at=now(), last_error=NULL WHERE id=?", integrationId);
    jdbc.update("INSERT INTO sync_state(integration_id,cursor) VALUES (?,?) ON CONFLICT (integration_id) DO UPDATE SET cursor=EXCLUDED.cursor, updated_at=now()", integrationId, newCursor);
    jdbc.update("UPDATE sync_history SET finished_at=now(), success=true, imported_count=? WHERE id=?", imported, historyId);
  }
  public void markError(UUID integrationId, UUID historyId, String message) {
    jdbc.update("UPDATE integrations SET status='ERROR', last_error=? WHERE id=?", message, integrationId);
    jdbc.update("UPDATE sync_history SET finished_at=now(), success=false, error=? WHERE id=?", message, historyId);
  }
  /** Deletes our stored tokens and cursor. The app stays authorised on the provider's side until the user revokes it there. */
  public void disconnect(UUID userId, Provider p) {
    jdbc.update("DELETE FROM oauth_tokens WHERE integration_id IN (SELECT id FROM integrations WHERE user_id=? AND provider=?)", userId, p.name());
    jdbc.update("DELETE FROM sync_state WHERE integration_id IN (SELECT id FROM integrations WHERE user_id=? AND provider=?)", userId, p.name());
    jdbc.update("UPDATE integrations SET status='DISCONNECTED', account_label=NULL, last_error=NULL WHERE user_id=? AND provider=?", userId, p.name());
  }
  public record Tokens(UUID integrationId, String accessEnc, String refreshEnc, Instant expiresAt) {}
  public Optional<Tokens> tokens(UUID userId, Provider p) {
    return jdbc.query("SELECT i.id,t.access_token_enc,t.refresh_token_enc,t.expires_at FROM oauth_tokens t JOIN integrations i ON i.id=t.integration_id WHERE i.user_id=? AND i.provider=?",
        (rs, n) -> new Tokens(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()), userId, p.name()).stream().findFirst();
  }
  /** Keeps an existing refresh token if the new grant did not include one. */
  public void saveTokens(UUID integrationId, String accessEnc, String refreshEnc, String scopes, Instant expiresAt) {
    jdbc.update("""
        INSERT INTO oauth_tokens(integration_id,access_token_enc,refresh_token_enc,scopes,expires_at) VALUES (?,?,?,?,?)
        ON CONFLICT (integration_id) DO UPDATE SET access_token_enc=EXCLUDED.access_token_enc,
        refresh_token_enc=COALESCE(EXCLUDED.refresh_token_enc, oauth_tokens.refresh_token_enc), scopes=EXCLUDED.scopes, expires_at=EXCLUDED.expires_at""",
        integrationId, accessEnc, refreshEnc, scopes, expiresAt == null ? null : Timestamp.from(expiresAt));
  }
  public void updateAccess(UUID integrationId, String accessEnc, Instant expiresAt) {
    jdbc.update("UPDATE oauth_tokens SET access_token_enc=?, expires_at=? WHERE integration_id=?", accessEnc, Timestamp.from(expiresAt), integrationId);
  }
  /** Users with a healthy connection who have automatic sync on. ERROR connections are skipped so revoked tokens are not retried. */
  public List<UUID> connectedUserIds(Provider p) {
    return jdbc.query("SELECT i.user_id FROM integrations i JOIN users u ON u.id=i.user_id WHERE i.provider=? AND i.status='CONNECTED' AND u.auto_sync", (rs, i) -> rs.getObject(1, UUID.class), p.name());
  }
}
