package com.digitaltimeline.task;
import com.digitaltimeline.task.TaskRepository.TaskDto;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;
/** Deterministic, explainable ranking: every point of the score has a plain-language reason. No AI involved. */
@Component
public class PriorityEngine {
  public record Scored(TaskDto task, int score, List<String> reasons) {}
  public Scored score(TaskDto t, Instant now) {
    int s = 0; List<String> why = new ArrayList<>();
    if (t.dueAt() != null) {
      long hours = Duration.between(now, t.dueAt()).toHours();
      if (t.dueAt().isBefore(now)) { s += 50; long d = Duration.between(t.dueAt(), now).toDays(); why.add(d > 0 ? "Overdue by " + d + " day(s)" : "Overdue"); }
      else if (hours <= 24) { s += 40; why.add("Due within 24 hours"); }
      else if (hours <= 72) { s += 25; why.add("Due within 3 days"); }
      else if (hours <= 168) { s += 10; why.add("Due within a week"); }
    }
    if (t.priority() == 1) { s += 20; why.add("Marked high priority"); } else if (t.priority() == 2) { s += 10; why.add("Marked medium priority"); }
    if ("IN_PROGRESS".equals(t.status())) { s += 5; why.add("Already in progress"); }
    if (why.isEmpty()) why.add("No deadline and low priority");
    return new Scored(t, s, why);
  }
  /** Top N open, user-confirmed tasks. A PriorityQueue (max-heap by score) hands back the most urgent first. */
  public List<Scored> top(List<TaskDto> tasks, Instant now, int limit) {
    PriorityQueue<Scored> pq = new PriorityQueue<>(Comparator.comparingInt(Scored::score).reversed().thenComparing(x -> x.task().title()));
    tasks.stream().filter(t -> !t.suggestion() && !"COMPLETED".equals(t.status())).map(t -> score(t, now)).forEach(pq::add);
    List<Scored> out = new ArrayList<>();
    while (!pq.isEmpty() && out.size() < limit) out.add(pq.poll());
    return out;
  }
}
