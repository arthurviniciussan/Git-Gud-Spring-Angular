package com.arthur.gitgud.article.domain;

import com.arthur.gitgud.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Um review no blog.
 *
 * <p>As regras de visibilidade moram aqui, nao no service: e o artigo que sabe
 * o que significa estar publicado.
 */
@Entity
@Table(name = "article")
@Getter
@Setter
@NoArgsConstructor
public class Article {

    private static final BigDecimal NOTA_MINIMA = BigDecimal.ZERO;
    private static final BigDecimal NOTA_MAXIMA = BigDecimal.TEN;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /** Endereco publico. Nao muda quando o titulo muda: link publicado precisa continuar valendo. */
    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    /** Vira a meta description e o texto do card na home. */
    @Column(nullable = false, length = 300)
    private String summary;

    /** A fonte da verdade: e isto que o editor edita. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_markdown", nullable = false)
    private String contentMarkdown;

    /** Derivado do markdown, ja sanitizado. O frontend so injeta. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** Nome do jogo analisado. Alimenta o JSON-LD de review na Etapa 5. */
    @Column(length = 120)
    private String game;

    @Column(precision = 3, scale = 1)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "article_tag",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    /**
     * Torna o artigo visivel.
     *
     * <p>A data e carimbada <b>uma vez</b>. Ela ordena a home e vai para o
     * sitemap: se pulasse para hoje a cada edicao, artigo antigo reapareceria
     * como novidade toda vez que eu corrigisse uma virgula.
     */
    public void publish(Instant agora) {
        this.status = ArticleStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = agora;
        }
    }

    /** Tira do ar sem apagar a data — republicar nao finge novidade. */
    public void unpublish() {
        this.status = ArticleStatus.DRAFT;
    }

    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }

    public void setScore(BigDecimal score) {
        if (score != null && (score.compareTo(NOTA_MINIMA) < 0 || score.compareTo(NOTA_MAXIMA) > 0)) {
            throw new BusinessException("A nota precisa estar entre 0 e 10. Recebido: " + score);
        }
        this.score = score;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }
}
