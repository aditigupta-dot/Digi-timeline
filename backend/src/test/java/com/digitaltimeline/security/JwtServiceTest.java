package com.digitaltimeline.security;
import static org.junit.jupiter.api.Assertions.*;
import com.digitaltimeline.config.AppProperties;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class JwtServiceTest {
  private static JwtService svc(String secret, int minutes) {
    return new JwtService(new AppProperties(new AppProperties.Jwt(secret, minutes, 7), "k", "http://x"));
  }
  private static final String SECRET = "0123456789-0123456789-0123456789-ab";
  @Test void roundTripReturnsSameUser() { var id = UUID.randomUUID(); var s = svc(SECRET, 5); assertEquals(id, s.parse(s.issue(id))); }
  @Test void tamperedTokenRejected() { var s = svc(SECRET, 5); assertThrows(JwtException.class, () -> s.parse(s.issue(UUID.randomUUID()) + "x")); }
  @Test void tokenFromOtherKeyRejected() {
    var t = svc(SECRET, 5).issue(UUID.randomUUID());
    assertThrows(JwtException.class, () -> svc("another-secret-another-secret-another!!", 5).parse(t));
  }
  @Test void expiredTokenRejected() { var s = svc(SECRET, -1); assertThrows(JwtException.class, () -> s.parse(s.issue(UUID.randomUUID()))); }
}
