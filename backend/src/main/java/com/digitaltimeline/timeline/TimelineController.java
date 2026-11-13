package com.digitaltimeline.timeline;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.activity.ActivityRepository.ActivityDto;
import com.digitaltimeline.activity.ImportedActivity;
import com.digitaltimeline.integration.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/timeline")
public class TimelineController {
  public record ManualActivity(@NotBlank String title, String description, Instant occurredAt) {}
  private final ActivityRepository activities;
  public TimelineController(ActivityRepository a) { this.activities = a; }
  @GetMapping List<ActivityDto> list(@AuthenticationPrincipal UUID u, @RequestParam(required = false) String source,
      @RequestParam(required = false) String q, @RequestParam(required = false) Instant before,
      @RequestParam(defaultValue = "30") int limit) { return activities.search(u, source, q, before, limit); }
  @GetMapping("/{id}") ActivityDto get(@AuthenticationPrincipal UUID u, @PathVariable UUID id) { return activities.get(u, id); }
  /** User-entered activity (source MANUAL) so the timeline works before any provider is connected. */
  @PostMapping("/manual") ActivityDto manual(@AuthenticationPrincipal UUID u, @Valid @RequestBody ManualActivity r) {
    String ext = UUID.randomUUID().toString();
    activities.saveAll(u, List.of(new ImportedActivity(Provider.MANUAL, ext, "NOTE", r.title(), r.description(), null,
        r.occurredAt() == null ? Instant.now() : r.occurredAt())));
    return activities.search(u, "MANUAL", null, null, 1).get(0);
  }
}
