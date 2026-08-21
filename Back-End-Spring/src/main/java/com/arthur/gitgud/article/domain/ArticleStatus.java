package com.arthur.gitgud.article.domain;

/**
 * Visibilidade do artigo.
 *
 * <p>{@code DRAFT} nao aparece em lugar nenhum da API publica — nem na lista,
 * nem pelo slug direto.
 */
public enum ArticleStatus {
    DRAFT,
    PUBLISHED
}
