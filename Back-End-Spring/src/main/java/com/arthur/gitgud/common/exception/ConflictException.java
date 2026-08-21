package com.arthur.gitgud.common.exception;

/** Estado atual do dado impede a operacao. Vira 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
