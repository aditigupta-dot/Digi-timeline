package com.digitaltimeline.integration;
import com.digitaltimeline.exception.OAuthException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
/** One-time, 10-minute OAuth `state` values bound to a user AND a provider (CSRF defence; a GitHub state cannot complete a Google callback). */
@Component
public class OAuthStateStore {
  private record Pending(UUID userId, Provider provider, Instant expires) {}
  private final Map<String, Pending> pending = new ConcurrentHashMap<>(); private final SecureRandom random = new SecureRandom();
  public String create(UUID userId, Provider provider) {
    pending.values().removeIf(p -> p.expires().isBefore(Instant.now()));
    byte[] b = new byte[24]; random.nextBytes(b);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    pending.put(state, new Pending(userId, provider, Instant.now().plusSeconds(600)));
    return state;
  }
  /** Returns the user and removes the state so it can never be replayed. */
  public UUID consume(String state, Provider provider) {
    Pending p = state == null ? null : pending.remove(state);
    if (p == null || p.expires().isBefore(Instant.now()) || p.provider() != provider) throw new OAuthException("Invalid or expired authorization state. Please try connecting again.");
    return p.userId();
  }
}
