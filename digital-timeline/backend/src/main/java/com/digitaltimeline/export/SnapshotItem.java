package com.digitaltimeline.export;

import com.digitaltimeline.domain.ActivityType;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;

/** SERIALIZATION: one activity inside a binary backup file. */
public class SnapshotItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String type;
    private final String title;
    private final String content;
    private final String project;
    private final Instant occurredAt;
    private final LinkedHashSet<String> tags;
    private final HashMap<String, String> attrs;

    /** transient = deliberately NOT written to the file. Embeddings are large and are recomputed on import. */
    private transient float[] embedding;

    public SnapshotItem(String type, String title, String content, String project, Instant occurredAt,
                        LinkedHashSet<String> tags, HashMap<String, String> attrs, float[] embedding) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.project = project;
        this.occurredAt = occurredAt;
        this.tags = tags;
        this.attrs = attrs;
        this.embedding = embedding;
    }

    /** Never trust a file: validate invariants while deserialising. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (type == null || title == null || title.isBlank() || occurredAt == null) {
            throw new InvalidObjectException("Snapshot item is missing required fields");
        }
        try {
            ActivityType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidObjectException("Unknown activity type: " + type);
        }
    }

    public String type() { return type; }
    public String title() { return title; }
    public String content() { return content; }
    public String project() { return project; }
    public Instant occurredAt() { return occurredAt; }
    public LinkedHashSet<String> tags() { return tags; }
    public HashMap<String, String> attrs() { return attrs; }
    public float[] embedding() { return embedding; }
}
