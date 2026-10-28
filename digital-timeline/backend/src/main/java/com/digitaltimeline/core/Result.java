package com.digitaltimeline.core;

import java.util.function.Function;

/** A tiny sealed "Ok or Err" type: explicit success/failure without exceptions for expected failures. */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    record Ok<T>(T value) implements Result<T> {}

    record Err<T>(String message, Throwable cause) implements Result<T> {}

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(String message, Throwable cause) {
        return new Err<>(message, cause);
    }

    default boolean isOk() {
        return this instanceof Ok<T>;
    }

    default T orElse(T fallback) {
        return this instanceof Ok<T> ok ? ok.value() : fallback;
    }

    default <R> Result<R> map(Function<? super T, ? extends R> fn) {
        return switch (this) {
            case Ok<T> ok -> new Ok<>(fn.apply(ok.value()));
            case Err<T> err -> new Err<>(err.message(), err.cause());
        };
    }
}
