package com.digitaltimeline.exception;

/** Raised when an LLM or embedding provider cannot be reached or returns an error. Callers degrade gracefully. */
public class AiProviderException extends DigitalTimelineException {
    public AiProviderException(String message) {
        super(message, 502);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, 502, cause);
    }
}
