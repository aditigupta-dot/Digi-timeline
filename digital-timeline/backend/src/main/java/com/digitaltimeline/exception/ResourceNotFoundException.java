package com.digitaltimeline.exception;

public class ResourceNotFoundException extends DigitalTimelineException {
    public ResourceNotFoundException(String what, Object id) {
        super(what + " not found: " + id, 404);
    }
}
