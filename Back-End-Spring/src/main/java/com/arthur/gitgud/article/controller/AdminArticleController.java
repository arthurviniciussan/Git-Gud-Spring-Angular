package com.arthur.gitgud.article.controller;

import com.arthur.gitgud.article.dto.AdminArticleResponse;
import com.arthur.gitgud.article.dto.ArticleRequest;
import com.arthur.gitgud.article.dto.PreviewRequest;
import com.arthur.gitgud.article.dto.PreviewResponse;
import com.arthur.gitgud.article.service.ArticleService;
import com.arthur.gitgud.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * O painel. Tudo aqui exige {@code ROLE_ADMIN} — a regra esta no SecurityConfig,
 * valendo para {@code /api/admin/**} inteiro.
 */
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** Lista tudo, rascunho incluso, da ultima edicao para tras. */
    @GetMapping
    public PageResponse<AdminArticleResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pagina = PageRequest.of(
                Math.max(page, 0), Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA));

        return PageResponse.of(articleService.listAll(pagina), AdminArticleResponse::from);
    }

    @GetMapping("/{id}")
    public AdminArticleResponse byId(@PathVariable String id) {
        return AdminArticleResponse.from(articleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AdminArticleResponse> create(@Valid @RequestBody ArticleRequest requisicao) {
        var criado = AdminArticleResponse.from(articleService.create(requisicao));

        return ResponseEntity
                .created(UriComponentsBuilder.fromPath("/api/admin/articles/{id}")
                        .buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @PutMapping("/{id}")
    public AdminArticleResponse update(@PathVariable String id,
                                       @Valid @RequestBody ArticleRequest requisicao) {
        return AdminArticleResponse.from(articleService.update(id, requisicao));
    }

    @PatchMapping("/{id}/publish")
    public AdminArticleResponse publish(@PathVariable String id) {
        return AdminArticleResponse.from(articleService.publish(id));
    }

    @PatchMapping("/{id}/unpublish")
    public AdminArticleResponse unpublish(@PathVariable String id) {
        return AdminArticleResponse.from(articleService.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        articleService.delete(id);
    }

    /**
     * Preview do editor.
     *
     * <p>Passa pela mesma conversao e sanitizacao do artigo salvo — se o preview
     * usasse outro caminho, mostraria uma coisa e publicaria outra.
     */
    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest requisicao) {
        return new PreviewResponse(articleService.renderPreview(requisicao.markdown()));
    }
}
