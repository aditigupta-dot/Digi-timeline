package com.digitaltimeline.exception;

public class AuthenticationFailedException extends DigitalTimelineException {
    public AuthenticationFailedException() {
        super("Invalid email or password", 401);
    }
}
