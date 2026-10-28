package com.digitaltimeline.core;

import java.time.Instant;

public record TimeRange(Instant from, Instant to, String label) {
    public boolean contains(Instant t) {
        return !t.isBefore(from) && t.isBefore(to);
    }
}
