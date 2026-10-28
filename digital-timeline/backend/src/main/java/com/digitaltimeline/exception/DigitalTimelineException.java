package com.digitaltimeline.exception;

/** Root of all unchecked application errors. Carries the HTTP status the API layer should answer with. */
public class DigitalTimelineException extends RuntimeException {
    private final int status;

    public DigitalTimelineException(String message, int status) {
        super(message);
        this.status = status;
    }

    public DigitalTimelineException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
