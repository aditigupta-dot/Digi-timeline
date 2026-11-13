package com.digitaltimeline.task;
import com.digitaltimeline.task.TaskRepository.TaskDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/tasks")
public class TaskController {
  public record CreateTask(@NotBlank @Size(max = 200) String title, String notes, @Min(1) @Max(3) Integer priority,
                           Instant dueAt, UUID projectId) {}
  public record UpdateTask(@Size(min = 1, max = 200) String title, String notes,
                           @Pattern(regexp = "PENDING|IN_PROGRESS|COMPLETED") String status,
                           @Min(1) @Max(3) Integer priority, Instant dueAt, Boolean confirmSuggestion) {}
  private final TaskRepository tasks;
  public TaskController(TaskRepository tasks) { this.tasks = tasks; }
  @GetMapping List<TaskDto> list(@AuthenticationPrincipal UUID u, @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String q) { return tasks.list(u, status, q); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  TaskDto create(@AuthenticationPrincipal UUID u, @Valid @RequestBody CreateTask r) {
    return tasks.create(u, r.title(), r.notes(), r.priority() == null ? 2 : r.priority(), r.dueAt(), r.projectId());
  }
  @PatchMapping("/{id}")
  TaskDto update(@AuthenticationPrincipal UUID u, @PathVariable UUID id, @Valid @RequestBody UpdateTask r) {
    return tasks.update(u, id, r.title(), r.notes(), r.status(), r.priority(), r.dueAt(), r.confirmSuggestion());
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal UUID u, @PathVariable UUID id) { tasks.delete(u, id); }
}
