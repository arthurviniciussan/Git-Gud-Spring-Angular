package com.arthur.gitgud.article.domain;

import com.arthur.gitgud.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleTest {

    private static final Instant PRIMEIRA_PUBLICACAO = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant DEPOIS = Instant.parse("2026-09-15T18:30:00Z");

    @Test
    @DisplayName("artigo novo nasce como rascunho, invisivel para o publico")
    void nasceComoRascunho() {
        Article artigo = artigo();

        assertThat(artigo.getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(artigo.isPublished()).isFalse();
        assertThat(artigo.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("publicar carimba a data e torna o artigo visivel")
    void publicarCarimbaData() {
        Article artigo = artigo();

        artigo.publish(PRIMEIRA_PUBLICACAO);

        assertThat(artigo.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(artigo.isPublished()).isTrue();
        assertThat(artigo.getPublishedAt()).isEqualTo(PRIMEIRA_PUBLICACAO);
    }

    @Test
    @DisplayName("publicar de novo nao reescreve a data original")
    void publicarDuasVezesMantemAPrimeiraData() {
        Article artigo = artigo();
        artigo.publish(PRIMEIRA_PUBLICACAO);

        artigo.publish(DEPOIS);

        // A data de publicacao ordena o blog e vai para o sitemap. Se ela pulasse
        // para hoje a cada edicao, artigo antigo apareceria como novidade.
        assertThat(artigo.getPublishedAt()).isEqualTo(PRIMEIRA_PUBLICACAO);
    }

    @Test
    @DisplayName("despublicar esconde do publico mas preserva a data")
    void despublicarPreservaData() {
        Article artigo = artigo();
        artigo.publish(PRIMEIRA_PUBLICACAO);

        artigo.unpublish();

        assertThat(artigo.getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(artigo.isPublished()).isFalse();
        assertThat(artigo.getPublishedAt()).isEqualTo(PRIMEIRA_PUBLICACAO);
    }

    @Test
    @DisplayName("republicar reaproveita a data original em vez de fingir novidade")
    void republicarMantemDataOriginal() {
        Article artigo = artigo();
        artigo.publish(PRIMEIRA_PUBLICACAO);
        artigo.unpublish();

        artigo.publish(DEPOIS);

        assertThat(artigo.getPublishedAt()).isEqualTo(PRIMEIRA_PUBLICACAO);
    }

    @Test
    @DisplayName("nota aceita o intervalo de 0 a 10")
    void aceitaNotaValida() {
        Article artigo = artigo();

        artigo.setScore(new BigDecimal("9.5"));
        assertThat(artigo.getScore()).isEqualByComparingTo("9.5");

        artigo.setScore(BigDecimal.ZERO);
        assertThat(artigo.getScore()).isEqualByComparingTo("0");

        artigo.setScore(null);
        assertThat(artigo.getScore()).isNull();
    }

    @Test
    @DisplayName("nota fora do intervalo e recusada no dominio, nao so no formulario")
    void recusaNotaForaDoIntervalo() {
        Article artigo = artigo();

        assertThatThrownBy(() -> artigo.setScore(new BigDecimal("10.1")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> artigo.setScore(new BigDecimal("-1")))
                .isInstanceOf(BusinessException.class);
    }

    private Article artigo() {
        Article artigo = new Article();
        artigo.setSlug(Slug.of("Elden Ring é difícil").value());
        artigo.setTitle("Elden Ring é difícil");
        artigo.setSummary("E tudo bem que seja.");
        artigo.setContentMarkdown("# Elden Ring");
        artigo.setContentHtml("<h1>Elden Ring</h1>");
        return artigo;
    }
}
