package com.digitaltimeline.core;

import java.util.function.LongSupplier;

/** Classic token-bucket rate limiter. Thread-safe; the clock is injectable so it is unit-testable. */
public final class TokenBucket {
    private final double capacity;
    private final double refillPerNano;
    private final LongSupplier nanoClock;
    private double tokens;
    private long lastRefill;

    public TokenBucket(double capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    public TokenBucket(double capacity, double refillPerSecond, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
        this.nanoClock = nanoClock;
        this.tokens = capacity;
        this.lastRefill = nanoClock.getAsLong();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /** Milliseconds until one token is available (0 if available now). */
    public synchronized long retryAfterMillis() {
        refill();
        if (tokens >= 1.0 || refillPerNano <= 0) return 0;
        return (long) Math.ceil(((1.0 - tokens) / refillPerNano) / 1_000_000.0);
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
        lastRefill = now;
    }
}
