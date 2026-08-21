package com.arthur.gitgud.article.dto;

import com.arthur.gitgud.article.domain.Article;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Item da listagem publica.
 *
 * <p>Deliberadamente sem o corpo do artigo: uma home com vinte reviews nao
 * precisa trafegar vinte textos completos.
 */
public record ArticleSummaryResponse(
        String slug,
        String title,
        String summary,
        String coverImageUrl,
        String game,
        BigDecimal score,
        Instant publishedAt,
        List<TagResponse> tags) {

    public static ArticleSummaryResponse from(Article artigo) {
        return new ArticleSummaryResponse(
                artigo.getSlug(),
                artigo.getTitle(),
                artigo.getSummary(),
                artigo.getCoverImageUrl(),
                artigo.getGame(),
                artigo.getScore(),
                artigo.getPublishedAt(),
                artigo.getTags().stream().map(TagResponse::from).toList());
    }
}
