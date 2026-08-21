package com.arthur.gitgud.article.dto;

/** HTML do preview, passado pela mesma sanitizacao do artigo salvo. */
public record PreviewResponse(String html) {
}
