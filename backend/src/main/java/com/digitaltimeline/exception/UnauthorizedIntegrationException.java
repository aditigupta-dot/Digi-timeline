package com.digitaltimeline.exception;
/** Provider rejected our token (expired/revoked): the user must reconnect. */
public class UnauthorizedIntegrationException extends RuntimeException { public UnauthorizedIntegrationException(String m) { super(m); } }
