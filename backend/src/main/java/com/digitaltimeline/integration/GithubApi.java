package com.digitaltimeline.integration;
import com.digitaltimeline.config.GithubProperties;
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
import org.springframework.stereotype.Component;
/** Thin HTTP adapter around GitHub's OAuth and REST endpoints. Translates HTTP failures into our own exceptions. */
@Component
public class GithubApi {
  private final GithubProperties props; private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public GithubApi(GithubProperties props, ObjectMapper json) { this.props = props; this.json = json; }
  private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
  public boolean configured() { return !props.clientId().isBlank() && !props.clientSecret().isBlank(); }
  public String authorizeUrl(String state) {
    return "https://github.com/login/oauth/authorize?client_id=" + enc(props.clientId()) + "&redirect_uri=" + enc(props.redirectUri())
        + "&scope=" + enc(props.scopes()) + "&state=" + enc(state);
  }
  /** Server-side code exchange: the client secret never leaves the backend. Returns the access token. */
  public String exchangeCode(String code) {
    try {
      String form = "client_id=" + enc(props.clientId()) + "&client_secret=" + enc(props.clientSecret()) + "&code=" + enc(code) + "&redirect_uri=" + enc(props.redirectUri());
      var req = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token")).header("Accept", "application/json")
          .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
      JsonNode n = json.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
      if (n.hasNonNull("error") || !n.hasNonNull("access_token")) throw new OAuthException("GitHub refused the authorization code: " + n.path("error_description").asText(n.path("error").asText("unknown error")));
      return n.get("access_token").asText();
    } catch (OAuthException e) { throw e; } catch (Exception e) { throw new OAuthException("Could not reach GitHub to finish connecting."); }
  }
  /** GET https://api.github.com{path}. Returns an empty array for 409 (empty repository). */
  public JsonNode get(String token, String path) {
    try {
      var req = HttpRequest.newBuilder(URI.create("https://api.github.com" + path)).header("Authorization", "Bearer " + token)
          .header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28").timeout(Duration.ofSeconds(20)).build();
      var res = http.send(req, HttpResponse.BodyHandlers.ofString());
      int s = res.statusCode();
      if (s == 401) throw new UnauthorizedIntegrationException("GitHub rejected the saved token. Please reconnect.");
      if (s == 409) return json.createArrayNode();
      if (s == 403 || s == 429) throw new SyncException("GitHub rate limit or permission problem (HTTP " + s + "). Try again later.");
      if (s / 100 != 2) throw new SyncException("GitHub returned HTTP " + s + " for " + path);
      return json.readTree(res.body());
    } catch (UnauthorizedIntegrationException | SyncException e) { throw e; }
    catch (Exception e) { throw new SyncException("Could not reach GitHub: " + e.getMessage(), e); }
  }
}
