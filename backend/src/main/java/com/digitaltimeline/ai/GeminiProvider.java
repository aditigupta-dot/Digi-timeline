package com.digitaltimeline.ai;
import com.digitaltimeline.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;
/**
 * Optional. Sends ONLY record titles, kinds, dates and short details (no email content, no descriptions) to Google's Gemini API,
 * and only when the user ticks the opt-in box. Free-tier usage and data terms are set by Google and can change.
 */
@Component
public class GeminiProvider implements AiProvider {
  private final AiProperties props; private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public GeminiProvider(AiProperties props, ObjectMapper json) { this.props = props; this.json = json; }
  public boolean configured() { return props.geminiApiKey() != null && !props.geminiApiKey().isBlank(); }
  @Override public String name() { return "gemini"; }
  @Override
  public String answer(String question, String intentLabel, List<Ref> ctx) {
    String records = IntStream.range(0, ctx.size()).mapToObj(i -> "[" + (i + 1) + "] " + ctx.get(i).kind() + " | " + ctx.get(i).title() + " | "
        + (ctx.get(i).at() == null ? "no date" : ctx.get(i).at()) + " | " + ctx.get(i).detail()).collect(Collectors.joining("\n"));
    String prompt = "You are the assistant inside a personal productivity app. Answer using ONLY the records below. Never invent tasks, deadlines or completion states. "
        + "If the records do not answer the question, say so plainly. Refer to records by their [number]. Keep it under 150 words.\n\nQuestion: " + question
        + "\nRetrieved: " + intentLabel + "\nRecords:\n" + (records.isEmpty() ? "(none)" : records);
    try {
      var body = json.createObjectNode(); body.putArray("contents").addObject().putArray("parts").addObject().put("text", prompt);
      var req = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + props.geminiModel() + ":generateContent"))
          .header("x-goog-api-key", props.geminiApiKey()).header("Content-Type", "application/json").timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
      var res = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) throw new IllegalStateException("Gemini returned HTTP " + res.statusCode());
      JsonNode text = json.readTree(res.body()).path("candidates").path(0).path("content").path("parts").path(0).path("text");
      if (text.isMissingNode()) throw new IllegalStateException("Gemini returned no text");
      return text.asText();
    } catch (IllegalStateException e) { throw e; } catch (Exception e) { throw new IllegalStateException("Could not reach Gemini", e); }
  }
}
