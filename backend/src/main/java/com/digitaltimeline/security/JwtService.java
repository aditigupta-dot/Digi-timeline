package com.digitaltimeline.security;
import com.digitaltimeline.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
/** Issues and validates short-lived access tokens. Throws JwtException (unchecked) if invalid/expired. */
@Service
public class JwtService {
  private final SecretKey key;
  private final int accessMinutes;
  public JwtService(AppProperties props) {
    this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8)); // needs >= 32 bytes
    this.accessMinutes = props.jwt().accessMinutes();
  }
  public String issue(UUID userId) {
    Instant now = Instant.now();
    return Jwts.builder().subject(userId.toString()).issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES))).signWith(key).compact();
  }
  public UUID parse(String token) {
    return UUID.fromString(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
  }
}
