package com.digitaltimeline.integration;
import com.digitaltimeline.config.GithubProperties;
import com.digitaltimeline.exception.OAuthException;
import com.digitaltimeline.exception.ResourceNotFoundException;
import com.digitaltimeline.security.TokenCipher;
import com.digitaltimeline.sync.SyncService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api")
public class IntegrationController {
  public record View(String provider, String status, String account, Instant lastSyncAt, String lastError, boolean syncing, boolean available) {}
  private final IntegrationRepository repo; private final OAuthStateStore states; private final GithubApi github; private final TokenCipher cipher;
  private final SyncService sync; private final GithubProperties props; private final GoogleOAuth google;
  public IntegrationController(IntegrationRepository repo, OAuthStateStore states, GithubApi github, TokenCipher cipher, SyncService sync, GithubProperties props, GoogleOAuth google) {
    this.google = google;
    this.repo = repo; this.states = states; this.github = github; this.cipher = cipher; this.sync = sync; this.props = props;
  }
  private static Provider parse(String s) {
    try { Provider p = Provider.valueOf(s.toUpperCase()); if (p == Provider.MANUAL) throw new IllegalArgumentException(); return p; }
    catch (IllegalArgumentException e) { throw new ResourceNotFoundException("Provider"); }
  }
  @GetMapping({"/integrations", "/sync/status"})
  List<View> list(@AuthenticationPrincipal UUID u) {
    Map<String, IntegrationRepository.Row> rows = repo.list(u).stream().collect(Collectors.toMap(IntegrationRepository.Row::provider, Function.identity()));
    return List.of(Provider.GITHUB, Provider.GMAIL, Provider.CALENDAR, Provider.CLASSROOM).stream().map(p -> {
      var r = rows.get(p.name());
      return new View(p.name(), r == null ? "DISCONNECTED" : r.status(), r == null ? null : r.account(), r == null ? null : r.lastSyncAt(),
          r == null ? null : r.lastError(), sync.isRunning(u, p), sync.supports(p)); }).toList();
  }
  @PostMapping("/integrations/{provider}/connect")
  Map<String, String> connect(@AuthenticationPrincipal UUID u, @PathVariable String provider) {
    Provider p = parse(provider);
    if (!sync.supports(p)) throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, p + " integration is not implemented yet.");
    if (p == Provider.GITHUB) {
      if (!github.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GitHub OAuth is not configured. Set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET (see README).");
      return Map.of("authorizeUrl", github.authorizeUrl(states.create(u, p)));
    }
    if (!google.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google OAuth is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET (see README).");
    return Map.of("authorizeUrl", google.authorizeUrl(p, states.create(u, p)));
  }
  /** Public endpoint (the browser arrives here from GitHub). The one-time `state` identifies the user. */
  @GetMapping("/integrations/{provider}/callback")
  ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam(required = false) String code,
                                @RequestParam(required = false) String state, @RequestParam(required = false) String error) {
    String base = props.frontendUrl() + "/app/integrations";
    try {
      Provider p = parse(provider);
      if (error != null) throw new OAuthException("Authorization was cancelled or denied at the provider.");
      UUID userId = states.consume(state, p);
      if (p == Provider.GITHUB) {
        String token = github.exchangeCode(code);
        UUID id = repo.upsertConnected(userId, p, github.get(token, "/user").path("login").asText());
        repo.saveTokens(id, cipher.encrypt(token), null, props.scopes());
      } else {
        var g = google.exchange(p, code);
        UUID id = repo.upsertConnected(userId, p, google.accountLabel(p, g.accessToken()));
        repo.saveTokens(id, cipher.encrypt(g.accessToken()), g.refreshToken() == null ? null : cipher.encrypt(g.refreshToken()), g.scopes(), g.expiresAt());
      }
      return redirect(base + "?connected=" + p.name().toLowerCase());
    } catch (OAuthException e) { return redirect(base + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
    } catch (RuntimeException e) { return redirect(base + "?error=" + URLEncoder.encode("Connection failed. Please try again.", StandardCharsets.UTF_8)); }
  }
  private static ResponseEntity<Void> redirect(String url) { return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build(); }
  @PostMapping("/integrations/{provider}/disconnect") @ResponseStatus(HttpStatus.NO_CONTENT)
  void disconnect(@AuthenticationPrincipal UUID u, @PathVariable String provider) { repo.disconnect(u, parse(provider)); }
  @PostMapping("/sync/{provider}") @ResponseStatus(HttpStatus.ACCEPTED)
  void syncNow(@AuthenticationPrincipal UUID u, @PathVariable String provider) {
    Provider p = parse(provider);
    if (repo.connectedId(u, p).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, p + " is not connected.");
    if (!sync.start(u, p)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A sync is already running.");
  }
}
