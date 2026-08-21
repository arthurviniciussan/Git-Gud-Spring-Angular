package com.arthur.gitgud.article.repository;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    Page<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status, Pageable pagina);

    @EntityGraph(attributePaths = "tags")
    Page<Article> findByStatusAndTags_SlugOrderByPublishedAtDesc(
            ArticleStatus status, String tagSlug, Pageable pagina);

    /**
     * Busca por titulo ou resumo.
     *
     * <p>Escrita a mao porque o nome derivado equivalente ficaria ilegivel. O
     * {@code LOWER} no titulo torna a comparacao insensivel a caixa sem depender
     * do collation do banco; o termo ja chega em minusculas do service.
     *
     * <p>O corpo do artigo fica de fora de proposito: {@code LIKE '%termo%'} em
     * LONGTEXT nao usa indice e ficaria lento conforme o blog cresce. Titulo e
     * resumo cobrem o que o leitor procura.
     */
    @Query("SELECT a FROM Article a "
            + "WHERE a.status = :status "
            + "  AND (LOWER(a.title) LIKE CONCAT('%', :termo, '%') "
            + "    OR LOWER(a.summary) LIKE CONCAT('%', :termo, '%')) "
            + "ORDER BY a.publishedAt DESC")
    @EntityGraph(attributePaths = "tags")
    Page<Article> search(@Param("status") ArticleStatus status,
                         @Param("termo") String termo,
                         Pageable pagina);

    /** Listagem do painel: inclui rascunho, ordenada pela ultima edicao. */
    @EntityGraph(attributePaths = "tags")
    Page<Article> findAllByOrderByUpdatedAtDesc(Pageable pagina);

    @EntityGraph(attributePaths = "tags")
    Optional<Article> findWithTagsById(String id);
}
