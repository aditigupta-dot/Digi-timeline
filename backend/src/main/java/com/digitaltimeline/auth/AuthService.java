package com.digitaltimeline.auth;
import com.digitaltimeline.config.AppProperties;
import com.digitaltimeline.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/** Register/login/refresh/logout. Refresh tokens are random, stored only as SHA-256 hashes, and rotated on use. */
@Service
public class AuthService {
  public record Tokens(String accessToken, String refreshToken) {}
  private final JdbcTemplate jdbc; private final PasswordEncoder encoder; private final JwtService jwt;
  private final int refreshDays; private final SecureRandom random = new SecureRandom();
  public AuthService(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, AppProperties props) {
    this.jdbc = jdbc; this.encoder = encoder; this.jwt = jwt; this.refreshDays = props.jwt().refreshDays();
  }
  @Transactional
  public Tokens register(String email, String name, String password) {
    String e = email.trim().toLowerCase();
    Integer n = jdbc.queryForObject("SELECT count(*) FROM users WHERE email=?", Integer.class, e);
    if (n != null && n > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    UUID id = jdbc.queryForObject("INSERT INTO users(email,display_name,password_hash) VALUES (?,?,?) RETURNING id",
        UUID.class, e, name.trim(), encoder.encode(password));
    return issue(id);
  }
  private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>(); // in-memory: per instance, resets on restart
  /** Allows 5 failed attempts per email per 15 minutes, then answers 429. */
  public Tokens login(String email, String password) {
    String key = email.trim().toLowerCase();
    Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
    Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
    recent.removeIf(t -> t.isBefore(cutoff));
    if (recent.size() >= 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Please wait a few minutes and try again.");
    try {
      var row = jdbc.queryForMap("SELECT id,password_hash FROM users WHERE email=?", key);
      if (encoder.matches(password, (String) row.get("password_hash"))) { failures.remove(key); return issue((UUID) row.get("id")); }
    } catch (EmptyResultDataAccessException ignored) { }
    recent.add(Instant.now());
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"); // same message either way
  }
  @Transactional
  public Tokens refresh(String refreshToken) {
    String h = hash(refreshToken);
    try {
      var row = jdbc.queryForMap("SELECT user_id FROM refresh_tokens WHERE token_hash=? AND NOT revoked AND expires_at>now()", h);
      jdbc.update("UPDATE refresh_tokens SET revoked=true WHERE token_hash=?", h); // rotation
      return issue((UUID) row.get("user_id"));
    } catch (EmptyResultDataAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
    }
  }
  public void logout(String refreshToken) { jdbc.update("UPDATE refresh_tokens SET revoked=true WHERE token_hash=?", hash(refreshToken)); }
  private Tokens issue(UUID userId) {
    byte[] raw = new byte[32]; random.nextBytes(raw);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    jdbc.update("INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (?,?,?)", userId, hash(refresh),
        Timestamp.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
    return new Tokens(jwt.issue(userId), refresh);
  }
  private static String hash(String s) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception e) { throw new IllegalStateException(e); }
  }
}
