package com.digitaltimeline.integration;
import com.digitaltimeline.exception.SyncException;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
/** Authenticated GET for any Google REST API, translating HTTP errors into our exceptions. */
@Component
public class GoogleApi {
  private final ObjectMapper json; private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public GoogleApi(ObjectMapper json) { this.json = json; }
  public JsonNode get(String token, String url) {
    try {
      var res = http.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token).timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
      int s = res.statusCode();
      if (s == 401) throw new UnauthorizedIntegrationException("Google rejected the saved token. Please reconnect.");
      if (s / 100 != 2) {
        String msg = json.readTree(res.body()).path("error").path("message").asText("no details");
        throw new SyncException("Google API returned HTTP " + s + ": " + msg + (s == 403 ? " (check the API is enabled in your Google Cloud project and this account is eligible)" : ""));
      }
      return json.readTree(res.body());
    } catch (UnauthorizedIntegrationException | SyncException e) { throw e; }
    catch (Exception e) { throw new SyncException("Could not reach Google: " + e.getMessage(), e); }
  }
}
