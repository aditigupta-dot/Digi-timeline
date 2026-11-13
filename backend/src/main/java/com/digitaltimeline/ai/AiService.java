package com.digitaltimeline.ai;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.activity.ActivityRepository.ActivityDto;
import com.digitaltimeline.ai.AiProvider.Ref;
import com.digitaltimeline.exception.ResourceNotFoundException;
import com.digitaltimeline.project.ProjectRepository;
import com.digitaltimeline.task.PriorityEngine;
import com.digitaltimeline.task.TaskRepository;
import com.digitaltimeline.task.TaskRepository.TaskDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
/** Pipeline: classify the question -> retrieve real records from the DB -> let a provider phrase the answer -> return the records as references. */
@Service
public class AiService {
  enum Intent { PROJECT, URGENT, DEADLINES, TODAY, WEEK, SEARCH }
  public record Answer(UUID conversationId, String answer, List<Ref> references, String provider) {}
  private record Retrieval(String label, List<Ref> refs) {}
  private static final Pattern DEADLINE = Pattern.compile("deadline|\\bdue\\b|overdue|upcoming|approach");
  private static final Set<String> STOP = Set.of("what", "show", "about", "with", "that", "this", "from", "have", "been", "were", "which", "summarize",
      "activities", "activity", "related", "tell", "please", "most", "working", "work", "does", "anything", "there");
  private final ActivityRepository activities; private final TaskRepository tasks; private final ProjectRepository projects;
  private final PriorityEngine engine; private final AiRepository repo; private final RuleBasedProvider rules; private final GeminiProvider gemini;
  public AiService(ActivityRepository a, TaskRepository t, ProjectRepository p, PriorityEngine e, AiRepository r, RuleBasedProvider rules, GeminiProvider gemini) {
    activities = a; tasks = t; projects = p; engine = e; repo = r; this.rules = rules; this.gemini = gemini;
  }
  public boolean externalAvailable() { return gemini.configured(); }
  static Intent classify(String question, boolean mentionsProject) {
    String q = question.toLowerCase();
    if (mentionsProject) return Intent.PROJECT;
    if (q.contains("urgent") || q.contains("priorit") || q.contains("important")) return Intent.URGENT;
    if (DEADLINE.matcher(q).find()) return Intent.DEADLINES;
    if (q.contains("today")) return Intent.TODAY;
    if (q.contains("week")) return Intent.WEEK;
    return Intent.SEARCH;
  }
  public Answer chat(UUID u, String message, UUID conversationId, boolean useExternal) {
    if (conversationId != null && !repo.owns(u, conversationId)) throw new ResourceNotFoundException("Conversation");
    Retrieval r = retrieve(u, message);
    AiProvider provider = useExternal && gemini.configured() ? gemini : rules; String used = provider.name(); String text;
    try { text = provider.answer(message, r.label(), r.refs()); }
    catch (RuntimeException e) { text = rules.answer(message, r.label(), r.refs()); used = "rule-based (Gemini unavailable)"; }
    UUID conv = conversationId != null ? conversationId : repo.createConversation(u);
    repo.addMessage(conv, "user", message, null);
    repo.addMessage(conv, "assistant", text, r.refs().stream().filter(x -> x.kind().equals("activity")).map(Ref::id).toList());
    return new Answer(conv, text, r.refs(), used);
  }
  private Retrieval retrieve(UUID u, String question) {
    String lower = question.toLowerCase(); Instant now = Instant.now();
    var project = projects.list(u).stream().filter(p -> lower.contains(p.name().toLowerCase())).findFirst();
    switch (classify(question, project.isPresent())) {
      case PROJECT -> { var p = project.get(); List<Ref> refs = new ArrayList<>();
        tasks.list(u, null, null).stream().filter(t -> p.id().equals(t.projectId())).forEach(t -> refs.add(taskRef(t, t.status())));
        activities.search(u, null, p.name(), null, 10).forEach(a -> refs.add(actRef(a)));
        return new Retrieval("Tasks in project \"" + p.name() + "\" and activities whose text matches its name", refs); }
      case URGENT -> { return new Retrieval("Your most urgent open tasks (ranked by due date and priority)",
          engine.top(tasks.list(u, null, null), now, 5).stream().map(s -> taskRef(s.task(), String.join("; ", s.reasons()))).toList()); }
      case DEADLINES -> { return new Retrieval("Open tasks due within 7 days or overdue", tasks.upcoming(u).stream().map(t -> taskRef(t, t.status())).toList()); }
      case TODAY -> { return new Retrieval("Your activity today", activities.since(u, LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), 20).stream().map(this::actRef).toList()); }
      case WEEK -> { return new Retrieval("Your activity over the last 7 days", activities.since(u, now.minus(7, ChronoUnit.DAYS), 30).stream().map(this::actRef).toList()); }
      default -> { return keywordSearch(u, lower); }
    }
  }
  private Retrieval keywordSearch(UUID u, String lower) {
    List<String> words = Arrays.stream(lower.split("\\W+")).filter(w -> w.length() > 3 && !STOP.contains(w)).distinct().limit(5).toList();
    Map<UUID, Ref> found = new LinkedHashMap<>();
    for (String w : words) {
      activities.search(u, null, w, null, 5).forEach(a -> found.putIfAbsent(a.id(), actRef(a)));
      tasks.list(u, null, w).stream().limit(3).forEach(t -> found.putIfAbsent(t.id(), taskRef(t, t.status())));
    }
    return new Retrieval("Records matching: " + String.join(", ", words), found.values().stream().limit(8).toList());
  }
  private Ref actRef(ActivityDto a) { return new Ref("activity", a.id(), a.title(), a.sourceUrl(), a.occurredAt(), a.source()); }
  private Ref taskRef(TaskDto t, String detail) { return new Ref("task", t.id(), t.title(), null, t.dueAt(), detail); }
}
