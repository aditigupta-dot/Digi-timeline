package com.digitaltimeline.exception;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
/** One JSON error shape for everything. Extending Spring's base class keeps bad JSON, wrong methods etc. as 4xx instead of 500. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  public record ApiError(String message, Instant at) {}
  private static ResponseEntity<ApiError> body(HttpStatusCode s, String m) { return ResponseEntity.status(s).body(new ApiError(m, Instant.now())); }
  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiError> notFound(ResourceNotFoundException e) { return body(HttpStatus.NOT_FOUND, e.getMessage()); }
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> status(ResponseStatusException e) { return body(e.getStatusCode(), e.getReason()); }
  @ExceptionHandler(Exception.class) // last resort: log details server-side, never leak them
  ResponseEntity<ApiError> other(Exception e) { log.error("Unhandled error", e); return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"); }
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders h, HttpStatusCode s, WebRequest r) {
    var f = ex.getBindingResult().getFieldError();
    return new ResponseEntity<>(new ApiError(f == null ? "Invalid request" : f.getField() + " " + f.getDefaultMessage(), Instant.now()), HttpStatus.BAD_REQUEST);
  }
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object b, HttpHeaders h, HttpStatusCode s, WebRequest r) {
    return new ResponseEntity<>(new ApiError(HttpStatus.valueOf(s.value()).getReasonPhrase(), Instant.now()), h, s);
  }
}
