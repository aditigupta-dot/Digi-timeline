package com.digitaltimeline.exception;
public class SyncException extends RuntimeException {
  public SyncException(String message, Throwable cause) { super(message, cause); }
  public SyncException(String message) { super(message); }
}
