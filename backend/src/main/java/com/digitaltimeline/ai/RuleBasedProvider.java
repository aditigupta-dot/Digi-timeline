package com.digitaltimeline.ai;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
/** Default provider: needs no key and cannot invent anything, because it only formats the records it is given. */
@Component
public class RuleBasedProvider implements AiProvider {
  private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault());
  @Override public String name() { return "rule-based"; }
  @Override
  public String answer(String question, String intentLabel, List<Ref> context) {
    if (context.isEmpty())
      return "I couldn't find anything in your imported data for that. I only answer from your own tasks and activities, so try syncing a source or adding entries.";
    return intentLabel + ":\n" + context.stream().map(r -> "• " + r.title() + " — " + r.detail() + (r.at() == null ? "" : ", " + WHEN.format(r.at()))).collect(Collectors.joining("\n"));
  }
}
