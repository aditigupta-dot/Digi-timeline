package com.digitaltimeline.exception;

public class RateLimitExceededException extends DigitalTimelineException {
    private final long retryAfterMillis;

    public RateLimitExceededException(long retryAfterMillis) {
        super("Too many requests, slow down", 429);
        this.retryAfterMillis = retryAfterMillis;
    }

    public long retryAfterMillis() {
        return retryAfterMillis;
    }
}
