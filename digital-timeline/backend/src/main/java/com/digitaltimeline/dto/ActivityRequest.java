package com.digitaltimeline.dto;

import com.digitaltimeline.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** attrs carries the type-specific fields (status, priority, dueAt, repo, url, location ...). */
public record ActivityRequest(
        @NotNull ActivityType type,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 10000) String content,
        Instant occurredAt,
        @Size(max = 120) String project,
        Set<String> tags,
        Map<String, String> attrs) {
}
