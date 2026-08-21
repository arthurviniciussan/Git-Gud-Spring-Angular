package com.arthur.gitgud.common.dto;

import java.time.Instant;

/**
 * Corpo unico de erro da API.
 *
 * <p>Toda falha sai neste formato, qualquer que seja o status. O cliente nao
 * precisa adivinhar o formato do erro dependendo do endpoint que chamou.
 */
public record ErrorResponse(String message, Instant timestamp) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Instant.now());
    }
}
