package com.digitaltimeline.task;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/tasks/priorities")
public class PriorityController {
  private final TaskRepository tasks; private final PriorityEngine engine;
  public PriorityController(TaskRepository tasks, PriorityEngine engine) { this.tasks = tasks; this.engine = engine; }
  @GetMapping List<PriorityEngine.Scored> top(@AuthenticationPrincipal UUID u, @RequestParam(defaultValue = "5") int limit) {
    return engine.top(tasks.list(u, null, null), Instant.now(), Math.min(Math.max(limit, 1), 20));
  }
}
