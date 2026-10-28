package com.digitaltimeline.service;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.DocumentActivity;
import com.digitaltimeline.domain.EventActivity;
import com.digitaltimeline.domain.GitHubActivity;
import com.digitaltimeline.domain.NoteActivity;
import com.digitaltimeline.domain.TaskActivity;
import com.digitaltimeline.domain.TaskStatus;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.exception.InvalidRequestException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** FACTORY pattern: turns a request DTO into the right Activity subclass. */
@Component
public class ActivityFactory {

    public Activity create(UUID userId, ActivityRequest r, String source, String externalId) {
        Activity a = switch (r.type()) {
            case TASK -> new TaskActivity();
            case NOTE -> new NoteActivity();
            case DOCUMENT -> new DocumentActivity();
            case GITHUB -> new GitHubActivity();
            case EVENT -> new EventActivity();
        };
        a.setUserId(userId);
        a.setSource(source);
        a.setExternalId(externalId);
        apply(a, r);
        return a;
    }

    /** Copies request fields (common and type-specific) onto an existing activity. Used for create and update. */
    public void apply(Activity a, ActivityRequest r) {
        a.setTitle(r.title().trim());
        a.setContent(r.content());
        a.setProject(r.project() == null || r.project().isBlank() ? null : r.project().trim());
        a.setOccurredAt(r.occurredAt() != null ? r.occurredAt() : (a.getOccurredAt() != null ? a.getOccurredAt() : Instant.now()));
        if (r.tags() != null) a.setTags(new LinkedHashSet<>(r.tags()));
        Map<String, String> attrs = r.attrs() == null ? Map.of() : r.attrs();

        if (a instanceof TaskActivity t) {
            t.setStatus(parseStatus(attrs.get("status"), t.getStatus()));
            t.setPriority(parseInt(attrs.get("priority"), t.getPriority() == null ? 2 : t.getPriority()));
            t.setDueAt(parseInstant(attrs.get("dueAt"), t.getDueAt()));
        } else if (a instanceof GitHubActivity g) {
            g.setRepo(attrs.getOrDefault("repo", g.getRepo()));
            g.setEventType(attrs.getOrDefault("eventType", g.getEventType()));
            g.setSha(attrs.getOrDefault("sha", g.getSha()));
            g.setUrl(attrs.getOrDefault("url", g.getUrl()));
        } else if (a instanceof DocumentActivity d) {
            d.setFilePath(attrs.getOrDefault("filePath", d.getFilePath()));
            d.setMimeType(attrs.getOrDefault("mimeType", d.getMimeType()));
            if (attrs.get("sizeBytes") != null) d.setSizeBytes(parseLong(attrs.get("sizeBytes")));
        } else if (a instanceof EventActivity e) {
            e.setLocation(attrs.getOrDefault("location", e.getLocation()));
            e.setAttendees(attrs.getOrDefault("attendees", e.getAttendees()));
            e.setEndsAt(parseInstant(attrs.get("endsAt"), e.getEndsAt()));
        } else if (a instanceof NoteActivity n) {
            n.setNoteFormat(attrs.getOrDefault("format", n.getNoteFormat()));
        }
    }

    private static TaskStatus parseStatus(String v, TaskStatus fallback) {
        if (v == null || v.isBlank()) return fallback == null ? TaskStatus.TODO : fallback;
        try {
            return TaskStatus.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown task status: " + v);
        }
    }

    private static Integer parseInt(String v, Integer fallback) {
        if (v == null || v.isBlank()) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Not a number: " + v);
        }
    }

    private static Long parseLong(String v) {
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Not a number: " + v);
        }
    }

    private static Instant parseInstant(String v, Instant fallback) {
        if (v == null || v.isBlank()) return fallback;
        try {
            return Instant.parse(v.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Use ISO-8601 like 2026-10-25T09:30:00Z, got: " + v);
        }
    }
}
