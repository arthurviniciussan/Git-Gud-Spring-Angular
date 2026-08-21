package com.arthur.gitgud.common.exception;

/** Regra de negocio violada. Vira 400. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
