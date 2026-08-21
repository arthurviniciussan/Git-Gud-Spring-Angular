package com.arthur.gitgud.common.exception;

/** Cliente excedeu um limite de frequencia. Vira 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
