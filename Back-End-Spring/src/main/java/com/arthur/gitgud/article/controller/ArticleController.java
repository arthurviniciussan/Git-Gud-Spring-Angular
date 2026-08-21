package com.arthur.gitgud.article.controller;

import com.arthur.gitgud.article.dto.ArticleResponse;
import com.arthur.gitgud.article.dto.ArticleSummaryResponse;
import com.arthur.gitgud.article.dto.TagResponse;
import com.arthur.gitgud.article.repository.TagRepository;
import com.arthur.gitgud.article.service.ArticleService;
import com.arthur.gitgud.common.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A parte do blog que qualquer pessoa le.
 *
 * <p>Sem token, sem conta, sem cadastro — e o ponto principal do produto. Estas
 * rotas sao as unicas publicas alem do login.
 */
@RestController
@RequestMapping("/api")
public class ArticleController {

    /** Teto de itens por pagina: o tamanho vem do cliente e nao pode virar um "me da tudo". */
    private static final int TAMANHO_MAXIMO_DA_PAGINA = 50;
    private static final int TAMANHO_PADRAO_DA_PAGINA = 10;

    private final ArticleService articleService;
    private final TagRepository tagRepository;

    public ArticleController(ArticleService articleService, TagRepository tagRepository) {
        this.articleService = articleService;
        this.tagRepository = tagRepository;
    }

    @GetMapping("/articles")
    public PageResponse<ArticleSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAMANHO_PADRAO_DA_PAGINA) int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q) {

        Pageable pagina = paginar(page, size);

        // A tag vem antes da busca: estando na pagina de uma tag, o filtro dela
        // manda. Combinar os dois so faria sentido com muito mais conteudo.
        var artigos = preenchido(tag) ? articleService.listPublishedByTag(tag, pagina)
                : preenchido(q) ? articleService.search(q, pagina)
                : articleService.listPublished(pagina);

        return PageResponse.of(artigos, ArticleSummaryResponse::from);
    }

    @GetMapping("/articles/{slug}")
    public ArticleResponse bySlug(@PathVariable String slug) {
        return ArticleResponse.from(articleService.findPublishedBySlug(slug));
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return tagRepository.findAllByOrderByNameAsc().stream().map(TagResponse::from).toList();
    }

    private boolean preenchido(String parametro) {
        return parametro != null && !parametro.isBlank();
    }

    private Pageable paginar(int page, int size) {
        int tamanho = Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA);
        return PageRequest.of(Math.max(page, 0), tamanho);
    }
}
