package com.arthur.gitgud.article;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import com.arthur.gitgud.article.dto.ArticleRequest;
import com.arthur.gitgud.article.repository.ArticleRepository;
import com.arthur.gitgud.article.repository.TagRepository;
import com.arthur.gitgud.article.service.ArticleService;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.support.IntegracaoComBancoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os artigos contra um MySQL real.
 *
 * <p>O que so aqui aparece: as migrations V3/V4 aplicadas de verdade, o
 * {@code ddl-auto: validate} conferindo {@code Article} e {@code Tag} contra o
 * schema, a constraint UNIQUE do slug e o CASCADE de {@code article_tag}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegracaoComBancoTest.class)
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
class ArticleFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void limpar() {
        articleRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("rascunho nao aparece na lista nem pelo endereco direto")
    void rascunhoInvisivel() throws Exception {
        articleService.create(requisicao("Elden Ring é difícil"));

        mvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/articles/elden-ring-e-dificil"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("publicado aparece para quem nao tem conta nenhuma")
    void publicadoVisivelSemConta() throws Exception {
        Article criado = articleService.create(requisicao("Elden Ring é difícil"));
        articleService.publish(criado.getId());

        mvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("elden-ring-e-dificil"));

        mvc.perform(get("/api/articles/elden-ring-e-dificil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentHtml", containsString("<strong>")))
                .andExpect(jsonPath("$.score").value(9.5));
    }

    @Test
    @DisplayName("titulos iguais convivem: o segundo ganha sufixo no endereco")
    void slugsNaoColidem() {
        articleService.create(requisicao("Elden Ring é difícil"));
        Article segundo = articleService.create(requisicao("Elden Ring é difícil"));

        // Sem o sufixo, a constraint uk_article_slug derrubaria a segunda criacao.
        assertThat(segundo.getSlug()).isEqualTo("elden-ring-e-dificil-2");
        assertThat(articleRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("a tag e criada uma vez e reaproveitada pelos artigos seguintes")
    void tagEReaproveitada() {
        articleService.create(requisicao("Primeiro artigo"));
        articleService.create(requisicao("Segundo artigo"));

        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(tagRepository.findBySlug("rpg")).isPresent();
    }

    @Test
    @DisplayName("filtrar por tag traz so os publicados daquela tag")
    void filtraPorTag() {
        Article publicado = articleService.create(requisicao("Publicado"));
        articleService.publish(publicado.getId());
        articleService.create(requisicao("Rascunho"));

        var pagina = articleService.listPublishedByTag("rpg", PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().getFirst().getTitle()).isEqualTo("Publicado");
    }

    @Test
    @DisplayName("apagar o artigo nao deixa vinculo orfao, e a tag sobrevive")
    void apagarNaoDeixaOrfao() {
        Article artigo = articleService.create(requisicao("Elden Ring"));

        articleService.delete(artigo.getId());

        assertThat(articleRepository.count()).isZero();
        // A tag pertence ao blog, nao ao artigo: ela continua para os proximos.
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("editar mantem o endereco e regenera o HTML")
    void editarMantemEndereco() {
        Article artigo = articleService.create(requisicao("Elden Ring é difícil"));
        articleService.publish(artigo.getId());

        Article atualizado = articleService.update(artigo.getId(), new ArticleRequest(
                "Elden Ring — revisto", "Novo resumo.", "Agora com *itálico*.",
                null, "Elden Ring", new BigDecimal("8.0"), List.of("RPG")));

        assertThat(atualizado.getSlug()).isEqualTo("elden-ring-e-dificil");
        assertThat(atualizado.getContentHtml()).contains("<em>itálico</em>");
        assertThat(atualizado.getScore()).isEqualByComparingTo("8.0");
    }

    @Test
    @DisplayName("despublicar tira do ar sem perder a data original")
    void despublicarPreservaData() throws Exception {
        Article artigo = articleService.create(requisicao("Elden Ring"));
        articleService.publish(artigo.getId());
        var dataOriginal = articleRepository.findById(artigo.getId()).orElseThrow().getPublishedAt();

        Article despublicado = articleService.unpublish(artigo.getId());

        assertThat(despublicado.getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(despublicado.getPublishedAt()).isEqualTo(dataOriginal);
        mvc.perform(get("/api/articles/elden-ring")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("texto longo cabe na coluna, sem truncar")
    void textoLongoCabe() {
        // Prova que content_markdown e mesmo LONGTEXT: um artigo grande nao pode
        // esbarrar no limite de 255 de um varchar.
        String textoGrande = "Uma frase razoavelmente longa sobre o jogo. ".repeat(3_000);

        Article artigo = articleService.create(new ArticleRequest(
                "Artigo enorme", "resumo", textoGrande, null, null, null, List.of()));

        String salvo = articleRepository.findById(artigo.getId()).orElseThrow().getContentMarkdown();
        assertThat(salvo).hasSameSizeAs(textoGrande);
    }

    private ArticleRequest requisicao(String titulo) {
        return new ArticleRequest(
                titulo, "Um resumo.", "Texto com **negrito**.",
                null, "Elden Ring", new BigDecimal("9.5"), List.of("RPG"));
    }
}
