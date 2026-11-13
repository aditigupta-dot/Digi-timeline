package com.digitaltimeline.ai;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
/** Strategy interface. Providers only ever see the records retrieved from the user's own data, never the whole database. */
public interface AiProvider {
  /** One retrieved record. `detail` is a short non-sensitive note (source name, status or ranking reasons). */
  record Ref(String kind, UUID id, String title, String url, Instant at, String detail) {}
  String name();
  String answer(String question, String intentLabel, List<Ref> context);
}
