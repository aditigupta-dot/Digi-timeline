package com.digitaltimeline.activity;
import com.digitaltimeline.integration.Provider;
import java.time.Instant;
/** Immutable record a connector returns; the repository dedupes on (user, source, externalId). */
public record ImportedActivity(Provider source, String externalId, String type, String title,
                               String description, String sourceUrl, Instant occurredAt) {}
