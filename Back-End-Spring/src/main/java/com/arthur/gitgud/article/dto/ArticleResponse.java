package com.arthur.gitgud.article.dto;

import com.arthur.gitgud.article.domain.Article;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Artigo completo para leitura publica.
 *
 * <p>Entrega {@code contentHtml}, nao o markdown: o HTML ja saiu sanitizado do
 * servidor e o frontend so precisa injetar.
 */
public record ArticleResponse(
        String slug,
        String title,
        String summary,
        String contentHtml,
        String coverImageUrl,
        String game,
        BigDecimal score,
        Instant publishedAt,
        List<TagResponse> tags) {

    public static ArticleResponse from(Article artigo) {
        return new ArticleResponse(
                artigo.getSlug(),
                artigo.getTitle(),
                artigo.getSummary(),
                artigo.getContentHtml(),
                artigo.getCoverImageUrl(),
                artigo.getGame(),
                artigo.getScore(),
                artigo.getPublishedAt(),
                artigo.getTags().stream().map(TagResponse::from).toList());
    }
}
