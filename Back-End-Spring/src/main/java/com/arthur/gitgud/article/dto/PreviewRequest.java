package com.arthur.gitgud.article.dto;

import jakarta.validation.constraints.NotNull;

/** Markdown que o editor quer ver renderizado enquanto escreve. */
public record PreviewRequest(@NotNull(message = "e obrigatorio") String markdown) {
}
