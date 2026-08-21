package com.arthur.gitgud.article.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Corpo de criacao e edicao de artigo. Chega apenas do painel de admin. */
public record ArticleRequest(
        @NotBlank(message = "e obrigatorio")
        @Size(max = 160, message = "no maximo 160 caracteres")
        String title,

        @NotBlank(message = "e obrigatorio")
        @Size(max = 300, message = "no maximo 300 caracteres")
        String summary,

        @NotBlank(message = "e obrigatorio")
        String contentMarkdown,

        @Size(max = 500, message = "no maximo 500 caracteres")
        String coverImageUrl,

        @Size(max = 120, message = "no maximo 120 caracteres")
        String game,

        @DecimalMin(value = "0.0", message = "minimo 0")
        @DecimalMax(value = "10.0", message = "maximo 10")
        BigDecimal score,

        List<String> tags) {

    public List<String> tags() {
        return tags == null ? List.of() : tags;
    }
}
