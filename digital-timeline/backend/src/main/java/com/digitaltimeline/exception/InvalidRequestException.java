package com.digitaltimeline.exception;

public class InvalidRequestException extends DigitalTimelineException {
    public InvalidRequestException(String message) {
        super(message, 400);
    }
}
