package com.digitaltimeline.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * ABSTRACTION + INHERITANCE: every kind of digital activity shares this base.
 * ENCAPSULATION: fields are private, state changes go through setters.
 * POLYMORPHISM: subclasses implement {@link #getType()} and {@link #describe()} differently.
 * JPA maps the whole hierarchy to ONE table ("activity") with a discriminator column "kind".
 */
@Entity
@Table(name = "activity", indexes = {
        @Index(name = "idx_activity_user_time", columnList = "user_id,occurred_at"),
        @Index(name = "idx_activity_external", columnList = "user_id,source,external_id")})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", length = 20)
public abstract class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 10000)
    private String content;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 60)
    private String source;

    @Column(name = "external_id", length = 300)
    private String externalId;

    @Column(length = 120)
    private String project;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_tags", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "tag", length = 60)
    @BatchSize(size = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Convert(converter = FloatVectorConverter.class)
    @Column(length = 16384)
    private float[] embedding;

    private double importance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (occurredAt == null) occurredAt = createdAt;
    }

    /** Abstract: each subclass knows its own type. */
    public abstract ActivityType getType();

    /** Abstract: a one-line human description used in AI prompts and the UI. */
    public abstract String describe();

    /** Text that represents this activity for embeddings and keyword search. */
    public String searchableText() {
        return String.join(" ", nullToEmpty(title), nullToEmpty(content), nullToEmpty(project),
                String.join(" ", tags), describe());
    }

    protected static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags); }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
    public Instant getCreatedAt() { return createdAt; }
}
