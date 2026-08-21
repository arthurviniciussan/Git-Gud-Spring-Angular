package com.arthur.gitgud.article.service;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import com.arthur.gitgud.article.domain.Slug;
import com.arthur.gitgud.article.domain.Tag;
import com.arthur.gitgud.article.dto.ArticleRequest;
import com.arthur.gitgud.article.repository.ArticleRepository;
import com.arthur.gitgud.article.repository.TagRepository;
import com.arthur.gitgud.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ArticleService {

    /**
     * Teto para a busca de um endereco livre.
     *
     * <p>Se cinquenta artigos ja disputam o mesmo titulo, o problema nao e o
     * slug — e melhor falhar alto do que varrer o banco indefinidamente.
     */
    private static final int MAXIMO_DE_TENTATIVAS_DE_SLUG = 50;

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final MarkdownRenderer markdownRenderer;
    private final Clock clock;

    public ArticleService(ArticleRepository articleRepository,
                          TagRepository tagRepository,
                          MarkdownRenderer markdownRenderer,
                          Clock clock) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.markdownRenderer = markdownRenderer;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- publico

    @Transactional(readOnly = true)
    public Page<Article> listPublished(Pageable pagina) {
        return articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED, pagina);
    }

    @Transactional(readOnly = true)
    public Page<Article> listPublishedByTag(String tagSlug, Pageable pagina) {
        return articleRepository.findByStatusAndTags_SlugOrderByPublishedAtDesc(
                ArticleStatus.PUBLISHED, tagSlug, pagina);
    }

    /**
     * Busca no blog.
     *
     * <p>Termo em branco cai na listagem normal: uma busca vazia significa "nao
     * estou filtrando nada", nao "me devolva tudo por LIKE '%%'".
     */
    @Transactional(readOnly = true)
    public Page<Article> search(String termo, Pageable pagina) {
        String normalizado = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);

        if (normalizado.isEmpty()) {
            return listPublished(pagina);
        }
        return articleRepository.search(ArticleStatus.PUBLISHED, normalizado, pagina);
    }

    /**
     * Artigo publicado pelo endereco.
     *
     * <p>Rascunho responde 404, nao 403: um 403 confirmaria que o artigo existe
     * e entregaria o rascunho antes da hora para quem chutasse o endereco.
     */
    @Transactional(readOnly = true)
    public Article findPublishedBySlug(String slug) {
        return articleRepository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Artigo nao encontrado: " + slug));
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public Page<Article> listAll(Pageable pagina) {
        return articleRepository.findAllByOrderByUpdatedAtDesc(pagina);
    }

    @Transactional(readOnly = true)
    public Article findById(String id) {
        return articleRepository.findWithTagsById(id)
                .orElseThrow(() -> naoEncontrado(id));
    }

    @Transactional
    public Article create(ArticleRequest requisicao) {
        Article artigo = new Article();
        artigo.setSlug(slugLivrePara(requisicao.title()));
        aplicar(requisicao, artigo);

        return articleRepository.save(artigo);
    }

    /**
     * Edita mantendo o endereco.
     *
     * <p>O slug nasce do titulo mas nao o segue: trocar o endereco quebraria todo
     * link ja compartilhado do artigo.
     */
    @Transactional
    public Article update(String id, ArticleRequest requisicao) {
        Article artigo = buscar(id);
        aplicar(requisicao, artigo);

        return articleRepository.save(artigo);
    }

    @Transactional
    public Article publish(String id) {
        Article artigo = buscar(id);
        artigo.publish(Instant.now(clock));
        artigo.setUpdatedAt(Instant.now(clock));

        return articleRepository.save(artigo);
    }

    @Transactional
    public Article unpublish(String id) {
        Article artigo = buscar(id);
        artigo.unpublish();
        artigo.setUpdatedAt(Instant.now(clock));

        return articleRepository.save(artigo);
    }

    @Transactional
    public void delete(String id) {
        articleRepository.delete(buscar(id));
    }

    /** Preview do editor: mesma conversao que o artigo salvo recebe. */
    public String renderPreview(String markdown) {
        return markdownRenderer.toHtml(markdown);
    }

    // ------------------------------------------------------------------ apoio

    private void aplicar(ArticleRequest requisicao, Article artigo) {
        artigo.setTitle(requisicao.title().trim());
        artigo.setSummary(requisicao.summary().trim());
        artigo.setContentMarkdown(requisicao.contentMarkdown());
        artigo.setContentHtml(markdownRenderer.toHtml(requisicao.contentMarkdown()));
        artigo.setCoverImageUrl(vazioViraNulo(requisicao.coverImageUrl()));
        artigo.setGame(vazioViraNulo(requisicao.game()));
        artigo.setScore(requisicao.score());
        artigo.setTags(resolverTags(requisicao.tags()));
        artigo.setUpdatedAt(Instant.now(clock));
    }

    /**
     * Encontra um endereco ainda nao usado.
     *
     * <p>Dois artigos podem legitimamente ter o mesmo titulo (uma re-analise, uma
     * parte 2). O sufixo evita que o segundo esbarre na constraint UNIQUE.
     */
    private String slugLivrePara(String titulo) {
        Slug base = Slug.of(titulo);

        if (!articleRepository.existsBySlug(base.value())) {
            return base.value();
        }

        for (int sufixo = 2; sufixo <= MAXIMO_DE_TENTATIVAS_DE_SLUG; sufixo++) {
            String candidato = base.comSufixo(sufixo).value();
            if (!articleRepository.existsBySlug(candidato)) {
                return candidato;
            }
        }

        throw new IllegalStateException(
                "Nao foi possivel gerar um endereco livre para o titulo: " + titulo);
    }

    /** Tag existente e reaproveitada; so a inedita e criada. */
    private Set<Tag> resolverTags(List<String> nomes) {
        Set<Tag> tags = new LinkedHashSet<>();

        for (String nome : nomes) {
            if (nome == null || nome.isBlank()) {
                continue;
            }
            String slug = Slug.of(nome).value();
            tags.add(tagRepository.findBySlug(slug)
                    .orElseGet(() -> tagRepository.save(Tag.of(nome))));
        }
        return tags;
    }

    private Article buscar(String id) {
        return articleRepository.findById(id).orElseThrow(() -> naoEncontrado(id));
    }

    private NotFoundException naoEncontrado(String id) {
        return new NotFoundException("Artigo nao encontrado: " + id);
    }

    private String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
