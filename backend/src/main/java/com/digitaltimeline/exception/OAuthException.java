package com.digitaltimeline.exception;
/** OAuth flow problem (bad state, failed code exchange). Message is safe to show the user. */
public class OAuthException extends RuntimeException { public OAuthException(String m) { super(m); } }
