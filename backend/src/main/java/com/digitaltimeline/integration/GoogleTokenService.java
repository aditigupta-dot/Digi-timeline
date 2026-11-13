package com.digitaltimeline.integration;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.digitaltimeline.security.TokenCipher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
/** Returns a valid access token, refreshing it with the stored refresh token when it is about to expire. */
@Service
public class GoogleTokenService {
  private final IntegrationRepository repo; private final GoogleOAuth oauth; private final TokenCipher cipher;
  public GoogleTokenService(IntegrationRepository repo, GoogleOAuth oauth, TokenCipher cipher) { this.repo = repo; this.oauth = oauth; this.cipher = cipher; }
  public String accessToken(UUID userId, Provider p) {
    var t = repo.tokens(userId, p).orElseThrow(() -> new UnauthorizedIntegrationException(p + " is not connected."));
    if (t.expiresAt() != null && t.expiresAt().isAfter(Instant.now().plusSeconds(60))) return cipher.decrypt(t.accessEnc());
    if (t.refreshEnc() == null) throw new UnauthorizedIntegrationException("No refresh token stored. Please reconnect.");
    var g = oauth.refresh(cipher.decrypt(t.refreshEnc()));
    repo.updateAccess(t.integrationId(), cipher.encrypt(g.accessToken()), g.expiresAt());
    return g.accessToken();
  }
}
