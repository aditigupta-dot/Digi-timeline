package com.digitaltimeline.task;
import static org.junit.jupiter.api.Assertions.*;
import com.digitaltimeline.task.TaskRepository.TaskDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class PriorityEngineTest {
  private final PriorityEngine engine = new PriorityEngine(); private final Instant now = Instant.parse("2026-11-13T10:00:00Z");
  private TaskDto task(String title, String status, int pri, Instant due, boolean suggestion) { return new TaskDto(UUID.randomUUID(), null, title, null, status, pri, due, suggestion); }
  @Test void overdueHighPriorityBeatsDueNextWeek() {
    var top = engine.top(List.of(task("later", "PENDING", 2, now.plus(Duration.ofDays(6)), false), task("late", "PENDING", 1, now.minus(Duration.ofDays(2)), false)), now, 5);
    assertEquals("late", top.get(0).task().title()); assertEquals(70, top.get(0).score());
  }
  @Test void suggestionsAndCompletedAreExcluded() {
    assertTrue(engine.top(List.of(task("s", "PENDING", 1, now, true), task("c", "COMPLETED", 1, now, false)), now, 5).isEmpty());
  }
  @Test void everyScoreHasAReason() { assertFalse(engine.score(task("x", "PENDING", 3, null, false), now).reasons().isEmpty()); }
  @Test void limitIsRespected() { var l = List.of(task("a", "PENDING", 1, null, false), task("b", "PENDING", 1, null, false), task("c", "PENDING", 1, null, false)); assertEquals(2, engine.top(l, now, 2).size()); }
}
