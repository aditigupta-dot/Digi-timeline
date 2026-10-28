package com.digitaltimeline.exception;

public class DuplicateResourceException extends DigitalTimelineException {
    public DuplicateResourceException(String message) {
        super(message, 409);
    }
}
