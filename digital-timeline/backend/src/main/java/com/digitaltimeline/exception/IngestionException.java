package com.digitaltimeline.exception;

/** CHECKED exception: collectors talk to the outside world, so callers are forced to handle failure. */
public class IngestionException extends Exception {
    private final String source;

    public IngestionException(String source, String message) {
        super(message);
        this.source = source;
    }

    public IngestionException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String source() {
        return source;
    }
}
