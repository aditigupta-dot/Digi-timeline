package com.digitaltimeline.web;

import com.digitaltimeline.service.ActivityCreatedEvent;
import com.digitaltimeline.service.ActivityLinkedEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes new activities and new graph links to the browser in real time (Server-Sent Events). */
@Service
public class TimelineStreamService {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(e -> list.remove(emitter));
        send(emitter, list, "hello", Map.of("ok", true));
        return emitter;
    }

    @EventListener
    public void onCreated(ActivityCreatedEvent event) {
        publish(event.userId(), "activity", event.activity());
    }

    @EventListener
    public void onLinked(ActivityLinkedEvent event) {
        publish(event.userId(), "linked", Map.of("id", event.activityId(), "links", event.links()));
    }

    private void publish(UUID userId, String name, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) return;
        for (SseEmitter e : list) send(e, list, name, data);
    }

    private void send(SseEmitter emitter, List<SseEmitter> list, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            list.remove(emitter);
        }
    }

    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        emitters.forEach((user, list) -> list.forEach(e -> {
            try {
                e.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                list.remove(e);
            }
        }));
    }
}
