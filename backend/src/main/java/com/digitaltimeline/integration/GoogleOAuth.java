package com.digitaltimeline.integration;
import com.digitaltimeline.config.GoogleProperties;
import com.digitaltimeline.exception.OAuthException;
import com.digitaltimeline.exception.SyncException;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
/** Google OAuth 2.0 authorization-code flow (server side). Each Google provider asks only for its own read-only scope(s). */
@Component
public class GoogleOAuth {
  public record Grant(String accessToken, String refreshToken, Instant expiresAt, String scopes) {}
  private static final Map<Provider, String> SCOPES = Map.of(
      Provider.GMAIL, "https://www.googleapis.com/auth/gmail.readonly",
      Provider.CALENDAR, "https://www.googleapis.com/auth/calendar.events.readonly",
      Provider.CLASSROOM, "https://www.googleapis.com/auth/classroom.courses.readonly https://www.googleapis.com/auth/classroom.coursework.me.readonly");
  private final GoogleProperties props; private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public GoogleOAuth(GoogleProperties props, ObjectMapper json) { this.props = props; this.json = json; }
  private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
  public boolean configured() { return !props.clientId().isBlank() && !props.clientSecret().isBlank(); }
  public String redirectUri(Provider p) { return props.redirectUriTemplate().replace("{provider}", p.name().toLowerCase()); }
  /** access_type=offline + prompt=consent make Google return a refresh token every time the user connects. */
  public String authorizeUrl(Provider p, String state) {
    return "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + enc(props.clientId()) + "&redirect_uri=" + enc(redirectUri(p))
        + "&response_type=code&scope=" + enc(SCOPES.get(p)) + "&access_type=offline&prompt=consent&state=" + enc(state);
  }
  public Grant exchange(Provider p, String code) {
    JsonNode n = post("code=" + enc(code) + "&client_id=" + enc(props.clientId()) + "&client_secret=" + enc(props.clientSecret())
        + "&redirect_uri=" + enc(redirectUri(p)) + "&grant_type=authorization_code");
    if (n.hasNonNull("error") || !n.hasNonNull("access_token")) throw new OAuthException("Google refused the authorization code: " + n.path("error_description").asText(n.path("error").asText("unknown error")));
    return grant(n);
  }
  /** invalid_grant means the user revoked access, or (app in Testing mode) the 7-day refresh token expired: reconnect needed. */
  public Grant refresh(String refreshToken) {
    JsonNode n = post("refresh_token=" + enc(refreshToken) + "&client_id=" + enc(props.clientId()) + "&client_secret=" + enc(props.clientSecret()) + "&grant_type=refresh_token");
    if ("invalid_grant".equals(n.path("error").asText())) throw new UnauthorizedIntegrationException("Google access expired or was revoked (apps in Testing mode lose access after 7 days). Please reconnect.");
    if (n.hasNonNull("error") || !n.hasNonNull("access_token")) throw new SyncException("Could not refresh the Google token: " + n.path("error").asText("unknown error"));
    return grant(n);
  }
  private static Grant grant(JsonNode n) {
    return new Grant(n.get("access_token").asText(), n.hasNonNull("refresh_token") ? n.get("refresh_token").asText() : null,
        Instant.now().plusSeconds(n.path("expires_in").asLong(3600)), n.path("scope").asText(""));
  }
  private JsonNode post(String form) {
    try {
      var req = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token")).header("Content-Type", "application/x-www-form-urlencoded")
          .timeout(Duration.ofSeconds(20)).POST(HttpRequest.BodyPublishers.ofString(form)).build();
      return json.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
    } catch (Exception e) { throw new SyncException("Could not reach Google: " + e.getMessage(), e); }
  }
  /** A friendly label for the Integrations page; failure here must not break connecting. */
  public String accountLabel(Provider p, String accessToken) {
    String url = switch (p) {
      case GMAIL -> "https://gmail.googleapis.com/gmail/v1/users/me/profile";
      case CALENDAR -> "https://www.googleapis.com/calendar/v3/calendars/primary";
      default -> null; };
    if (url == null) return "Google account";
    try {
      var res = http.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + accessToken).build(), HttpResponse.BodyHandlers.ofString());
      JsonNode n = json.readTree(res.body());
      return n.path(p == Provider.GMAIL ? "emailAddress" : "id").asText("Google account");
    } catch (Exception e) { return "Google account"; }
  }
}
