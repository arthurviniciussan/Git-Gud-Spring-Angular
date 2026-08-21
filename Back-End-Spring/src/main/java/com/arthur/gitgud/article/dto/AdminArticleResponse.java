package com.arthur.gitgud.article.dto;

import com.arthur.gitgud.article.domain.Article;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Artigo como o painel precisa ver.
 *
 * <p>Diferente da resposta publica em dois pontos: traz o {@code id} e o
 * markdown (o editor edita a fonte, nao o HTML), e expoe o {@code status} —
 * que e justamente o que a API publica nunca revela.
 */
public record AdminArticleResponse(
        String id,
        String slug,
        String title,
        String summary,
        String contentMarkdown,
        String coverImageUrl,
        String game,
        BigDecimal score,
        String status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TagResponse> tags) {

    public static AdminArticleResponse from(Article artigo) {
        return new AdminArticleResponse(
                artigo.getId(),
                artigo.getSlug(),
                artigo.getTitle(),
                artigo.getSummary(),
                artigo.getContentMarkdown(),
                artigo.getCoverImageUrl(),
                artigo.getGame(),
                artigo.getScore(),
                artigo.getStatus().name(),
                artigo.getPublishedAt(),
                artigo.getCreatedAt(),
                artigo.getUpdatedAt(),
                artigo.getTags().stream().map(TagResponse::from).toList());
    }
}
