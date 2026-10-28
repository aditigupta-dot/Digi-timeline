package com.digitaltimeline.dto;

import com.digitaltimeline.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiDtos {
    private AiDtos() {}

    public record AskRequest(@NotBlank @Size(max = 1000) String question, String tz) {}

    /** One piece of evidence behind an answer. ref is the [A1], [A2]... label used in the answer text. */
    public record Citation(String ref, UUID id, String title, ActivityType type, Instant occurredAt, double score,
                           String why, boolean citedInAnswer) {}

    public record AskResponse(String answer, List<Citation> citations, String timeRange, String mode,
                              List<String> followUps, int redactions) {}

    public record SearchHit(ActivityResponse activity, double score, String why) {}
}
