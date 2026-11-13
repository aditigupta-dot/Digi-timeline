package com.digitaltimeline.notification;
import com.digitaltimeline.sync.SyncFinishedEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/** Keeps each user's open SSE connections and pushes sync results to them. Observer of SyncFinishedEvent. */
@Component
public class SseHub {
  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
  public SseEmitter subscribe(UUID userId) {
    SseEmitter em = new SseEmitter(30 * 60 * 1000L); // browser reconnects after 30 min
    List<SseEmitter> mine = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
    mine.add(em);
    em.onCompletion(() -> mine.remove(em)); em.onTimeout(() -> mine.remove(em)); em.onError(t -> mine.remove(em));
    try { em.send(SseEmitter.event().name("ready").data(Map.of("ok", true), MediaType.APPLICATION_JSON)); } catch (IOException e) { mine.remove(em); }
    return em;
  }
  public void send(UUID userId, String event, Object data) {
    List<SseEmitter> mine = emitters.get(userId);
    if (mine == null) return;
    for (SseEmitter em : mine) {
      try { em.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON)); } catch (Exception e) { mine.remove(em); }
    }
  }
  @EventListener
  void on(SyncFinishedEvent e) {
    send(e.userId(), "sync", Map.of("provider", e.provider().name(), "imported", e.imported(), "success", e.success(), "error", e.error() == null ? "" : e.error()));
  }
  @Scheduled(fixedRate = 25000) // keeps proxies from closing idle streams
  void heartbeat() {
    emitters.values().forEach(list -> list.forEach(em -> { try { em.send(SseEmitter.event().comment("ping")); } catch (Exception e) { list.remove(em); } }));
  }
}
