package com.digitaltimeline.dto;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.DocumentActivity;
import com.digitaltimeline.domain.EventActivity;
import com.digitaltimeline.domain.GitHubActivity;
import com.digitaltimeline.domain.NoteActivity;
import com.digitaltimeline.domain.TaskActivity;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public record ActivityResponse(
        UUID id, ActivityType type, String title, String content, Instant occurredAt, String source,
        String project, Set<String> tags, double importance, Map<String, Object> details) {

    public static ActivityResponse from(Activity a) {
        Map<String, Object> d = new LinkedHashMap<>();
        // Pattern matching on the runtime subtype (polymorphism, Java 21 style)
        if (a instanceof TaskActivity t) {
            d.put("status", t.getStatus());
            d.put("priority", t.getPriority());
            d.put("dueAt", t.getDueAt());
        } else if (a instanceof GitHubActivity g) {
            d.put("repo", g.getRepo());
            d.put("eventType", g.getEventType());
            d.put("sha", g.getSha());
            d.put("url", g.getUrl());
        } else if (a instanceof DocumentActivity doc) {
            d.put("filePath", doc.getFilePath());
            d.put("mimeType", doc.getMimeType());
            d.put("sizeBytes", doc.getSizeBytes());
        } else if (a instanceof EventActivity e) {
            d.put("endsAt", e.getEndsAt());
            d.put("location", e.getLocation());
            d.put("attendees", e.getAttendees());
        } else if (a instanceof NoteActivity n) {
            d.put("format", n.getNoteFormat());
        }
        return new ActivityResponse(a.getId(), a.getType(), a.getTitle(), a.getContent(), a.getOccurredAt(),
                a.getSource(), a.getProject(), new TreeSet<>(a.getTags()), a.getImportance(), d);
    }
}
