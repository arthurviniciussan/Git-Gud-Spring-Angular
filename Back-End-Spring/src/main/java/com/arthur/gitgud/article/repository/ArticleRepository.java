package com.arthur.gitgud.article.repository;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {

    boolean existsBySlug(String slug);

    /**
     * Busca publica.
     *
     * <p>O status entra na consulta de proposito: sem ele, um rascunho voltaria
     * do banco e sobraria para o service lembrar de escondê-lo.
     */
    @EntityGraph(attributePaths = "tags")
    Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);

    @EntityGraph(attributePaths = "tags")
    Page<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    Page<Article> findByStatusAndTags_SlugOrderByPublishedAtDesc(
            ArticleStatus status, String tagSlug, Pageable pageable);

    /** Listagem do painel: inclui rascunho, ordenada pela ultima edicao. */
    @EntityGraph(attributePaths = "tags")
    Page<Article> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    Optional<Article> findWithTagsById(String id);
}
