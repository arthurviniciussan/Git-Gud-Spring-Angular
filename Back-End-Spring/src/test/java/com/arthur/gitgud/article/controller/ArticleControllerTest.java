package com.arthur.gitgud.article.controller;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import com.arthur.gitgud.article.domain.Tag;
import com.arthur.gitgud.article.repository.TagRepository;
import com.arthur.gitgud.article.service.ArticleService;
import com.arthur.gitgud.auth.security.LoginRateLimiter;
import com.arthur.gitgud.auth.security.SecurityConfig;
import com.arthur.gitgud.auth.security.SecurityFilter;
import com.arthur.gitgud.auth.service.TokenService;
import com.arthur.gitgud.common.exception.NotFoundException;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A fronteira que define o produto: qualquer pessoa le, so o admin escreve.
 */
@WebMvcTest({ArticleController.class, AdminArticleController.class})
@EnableConfigurationProperties(GitgudProperties.class)
@Import({SecurityConfig.class, SecurityFilter.class, TokenService.class, LoginRateLimiter.class})
@TestPropertySource(properties = {
        "gitgud.admin.email=admin@gitgud.dev",
        "gitgud.admin.name=Arthur",
        "gitgud.admin.password-hash=" + Fixtures.HASH_VALIDO,
        "gitgud.jwt.secret=" + Fixtures.SEGREDO_JWT,
        "gitgud.jwt.issuer=gitgud-api",
        "gitgud.jwt.expiration=2h",
        "gitgud.cors.allowed-origins=http://localhost:4200",
        "gitgud.login.max-attempts=5",
        "gitgud.login.lock-duration=15m"
})
class ArticleControllerTest {

    private static final String CORPO_VALIDO = """
            {"title":"Elden Ring é difícil","summary":"E tudo bem.",
             "contentMarkdown":"# Oi","tags":["RPG"]}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void preparar() {
        var admin = Fixtures.admin("admin@gitgud.dev");
        when(userRepository.findByEmail("admin@gitgud.dev")).thenReturn(Optional.of(admin));
        token = tokenService.generateToken(admin);
    }

    // ------------------------------------------------------------- leitura publica

    @Test
    @DisplayName("visitante sem conta le a lista de artigos")
    void listaEPublica() throws Exception {
        when(articleService.listPublished(any(Pageable.class))).thenReturn(pagina(artigoPublicado()));

        mvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("elden-ring"))
                .andExpect(jsonPath("$.content[0].score").value(9.5))
                .andExpect(jsonPath("$.content[0].tags[0].slug").value("rpg"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("a listagem nao carrega o corpo dos artigos")
    void listaNaoTrazCorpo() throws Exception {
        when(articleService.listPublished(any(Pageable.class))).thenReturn(pagina(artigoPublicado()));

        mvc.perform(get("/api/articles"))
                .andExpect(jsonPath("$.content[0].contentHtml").doesNotExist())
                .andExpect(jsonPath("$.content[0].contentMarkdown").doesNotExist());
    }

    @Test
    @DisplayName("visitante sem conta le o artigo pelo endereco")
    void artigoEPublico() throws Exception {
        when(articleService.findPublishedBySlug("elden-ring")).thenReturn(artigoPublicado());

        mvc.perform(get("/api/articles/elden-ring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Elden Ring é difícil"))
                .andExpect(jsonPath("$.contentHtml").value("<p>texto</p>"))
                // status e markdown sao assunto do painel, nao do leitor.
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.contentMarkdown").doesNotExist());
    }

    @Test
    @DisplayName("rascunho responde 404 pelo endereco publico")
    void rascunhoDaQuatrocentosEQuatro() throws Exception {
        when(articleService.findPublishedBySlug("rascunho"))
                .thenThrow(new NotFoundException("Artigo nao encontrado: rascunho"));

        mvc.perform(get("/api/articles/rascunho"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("filtrar por tag usa a consulta por tag")
    void filtraPorTag() throws Exception {
        when(articleService.listPublishedByTag(eq("rpg"), any(Pageable.class)))
                .thenReturn(pagina(artigoPublicado()));

        mvc.perform(get("/api/articles").param("tag", "rpg"))
                .andExpect(status().isOk());

        verify(articleService).listPublishedByTag(eq("rpg"), any(Pageable.class));
    }

    @Test
    @DisplayName("busca no site usa a consulta de busca")
    void buscaUsaConsultaDeBusca() throws Exception {
        when(articleService.search(eq("elden"), any(Pageable.class))).thenReturn(pagina());

        mvc.perform(get("/api/articles").param("q", "elden")).andExpect(status().isOk());

        verify(articleService).search(eq("elden"), any(Pageable.class));
    }

    @Test
    @DisplayName("a busca tambem e publica, sem token")
    void buscaEPublica() throws Exception {
        when(articleService.search(eq("elden"), any(Pageable.class)))
                .thenReturn(pagina(artigoPublicado()));

        mvc.perform(get("/api/articles").param("q", "elden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("elden-ring"));
    }

    @Test
    @DisplayName("tamanho de pagina exagerado e limitado no servidor")
    void limitaTamanhoDaPagina() throws Exception {
        when(articleService.listPublished(any(Pageable.class))).thenReturn(pagina());

        mvc.perform(get("/api/articles").param("size", "5000")).andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(articleService).listPublished(captor.capture());
        // Sem teto, "size=5000" viraria um pedido para o banco despejar tudo.
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("lista de tags e publica")
    void tagsSaoPublicas() throws Exception {
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(Tag.of("RPG")));

        mvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("rpg"));
    }

    // ------------------------------------------------------------------ escrita

    @Test
    @DisplayName("criar artigo sem token responde 401")
    void criarSemTokenNaoPassa() throws Exception {
        mvc.perform(post("/api/admin/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("publicar sem token responde 401")
    void publicarSemTokenNaoPassa() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/articles/id-1/publish"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("listar rascunhos sem token responde 401")
    void listarNoPainelSemTokenNaoPassa() throws Exception {
        mvc.perform(get("/api/admin/articles")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin autenticado cria o artigo e recebe o Location")
    void adminCria() throws Exception {
        when(articleService.create(any())).thenReturn(artigoPublicado());

        mvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Location", "/api/admin/articles/id-1"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.contentMarkdown").value("texto"));
    }

    @Test
    @DisplayName("titulo em branco para na validacao, antes do service")
    void validaTituloObrigatorio() throws Exception {
        mvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  ","summary":"resumo","contentMarkdown":"texto"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: e obrigatorio"));
    }

    @Test
    @DisplayName("nota fora do intervalo e recusada na entrada")
    void validaNota() throws Exception {
        mvc.perform(post("/api/admin/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"T","summary":"r","contentMarkdown":"t","score":11}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("score: maximo 10"));
    }

    @Test
    @DisplayName("preview usa a mesma sanitizacao do artigo salvo")
    void previewSanitiza() throws Exception {
        when(articleService.renderPreview(anyString())).thenReturn("<p>ok</p>");

        mvc.perform(post("/api/admin/articles/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"markdown":"# Oi"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value("<p>ok</p>"));
    }

    @Test
    @DisplayName("preview sem token responde 401")
    void previewSemTokenNaoPassa() throws Exception {
        mvc.perform(post("/api/admin/articles/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"markdown":"# Oi"}"""))
                .andExpect(status().isUnauthorized());
    }

    private Page<Article> pagina(Article... artigos) {
        return new PageImpl<>(List.of(artigos), PageRequest.of(0, 10), artigos.length);
    }

    private Article artigoPublicado() {
        Article artigo = new Article();
        artigo.setId("id-1");
        artigo.setSlug("elden-ring");
        artigo.setTitle("Elden Ring é difícil");
        artigo.setSummary("E tudo bem.");
        artigo.setContentMarkdown("texto");
        artigo.setContentHtml("<p>texto</p>");
        artigo.setGame("Elden Ring");
        artigo.setScore(new BigDecimal("9.5"));
        artigo.addTag(Tag.of("RPG"));
        artigo.publish(Instant.parse("2026-08-01T10:00:00Z"));
        return artigo;
    }
}
