package com.digitaltimeline.notification;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController @RequestMapping("/api")
public class NotificationController {
  public record Listing(List<NotificationRepository.Dto> items, int unread) {}
  private final NotificationRepository repo; private final SseHub hub;
  public NotificationController(NotificationRepository repo, SseHub hub) { this.repo = repo; this.hub = hub; }
  @GetMapping("/notifications") Listing list(@AuthenticationPrincipal UUID u) { return new Listing(repo.recent(u), repo.unread(u)); }
  @PostMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) void readAll(@AuthenticationPrincipal UUID u) { repo.markAllRead(u); }
  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE) SseEmitter events(@AuthenticationPrincipal UUID u) { return hub.subscribe(u); }
}
