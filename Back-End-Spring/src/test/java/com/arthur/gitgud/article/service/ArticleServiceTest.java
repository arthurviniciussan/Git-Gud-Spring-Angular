package com.arthur.gitgud.article.service;

import com.arthur.gitgud.article.domain.Article;
import com.arthur.gitgud.article.domain.ArticleStatus;
import com.arthur.gitgud.article.domain.Tag;
import com.arthur.gitgud.article.dto.ArticleRequest;
import com.arthur.gitgud.article.repository.ArticleRepository;
import com.arthur.gitgud.article.repository.TagRepository;
import com.arthur.gitgud.common.exception.NotFoundException;
import com.arthur.gitgud.support.RelogioDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-21T12:00:00Z");

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private TagRepository tagRepository;

    private ArticleService service;

    @BeforeEach
    void preparar() {
        service = new ArticleService(
                articleRepository, tagRepository, new MarkdownRenderer(), new RelogioDeTeste(AGORA));

        when(articleRepository.save(any(Article.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(tagRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    @DisplayName("criar deriva o slug do titulo e renderiza o markdown")
    void criarDerivaSlugERenderiza() {
        Article criado = service.create(requisicao("Elden Ring é difícil", "# Título\n\nTexto **forte**."));

        assertThat(criado.getSlug()).isEqualTo("elden-ring-e-dificil");
        assertThat(criado.getContentHtml()).contains("<strong>forte</strong>");
        assertThat(criado.getContentMarkdown()).contains("**forte**");
        assertThat(criado.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    @DisplayName("titulo repetido ganha sufixo em vez de estourar a constraint")
    void slugRepetidoGanhaSufixo() {
        when(articleRepository.existsBySlug("elden-ring")).thenReturn(true);

        Article criado = service.create(requisicao("Elden Ring", "texto"));

        assertThat(criado.getSlug()).isEqualTo("elden-ring-2");
    }

    @Test
    @DisplayName("o sufixo avanca ate achar um endereco livre")
    void sufixoAvancaAteVagar() {
        when(articleRepository.existsBySlug("elden-ring")).thenReturn(true);
        when(articleRepository.existsBySlug("elden-ring-2")).thenReturn(true);
        when(articleRepository.existsBySlug("elden-ring-3")).thenReturn(true);

        Article criado = service.create(requisicao("Elden Ring", "texto"));

        assertThat(criado.getSlug()).isEqualTo("elden-ring-4");
    }

    @Test
    @DisplayName("editar regenera o HTML mas preserva o endereco publicado")
    void editarPreservaSlug() {
        Article existente = artigoSalvo("elden-ring", ArticleStatus.PUBLISHED);
        when(articleRepository.findById("id-1")).thenReturn(Optional.of(existente));

        Article atualizado = service.update("id-1",
                requisicao("Elden Ring — revisado em 2026", "Agora com *itálico*."));

        // Trocar o slug quebraria todo link ja compartilhado do artigo.
        assertThat(atualizado.getSlug()).isEqualTo("elden-ring");
        assertThat(atualizado.getTitle()).isEqualTo("Elden Ring — revisado em 2026");
        assertThat(atualizado.getContentHtml()).contains("<em>itálico</em>");
    }

    @Test
    @DisplayName("publicar carimba a data com o relogio da aplicacao")
    void publicarCarimbaData() {
        Article existente = artigoSalvo("elden-ring", ArticleStatus.DRAFT);
        when(articleRepository.findById("id-1")).thenReturn(Optional.of(existente));

        Article publicado = service.publish("id-1");

        assertThat(publicado.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(publicado.getPublishedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("rascunho responde 404 pelo slug direto, nao 403")
    void rascunhoNaoVazaPeloSlug() {
        when(articleRepository.findBySlugAndStatus("elden-ring", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        // 403 confirmaria que o artigo existe. 404 nao conta nada a ninguem.
        assertThatThrownBy(() -> service.findPublishedBySlug("elden-ring"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("artigo inexistente para admin tambem e 404")
    void artigoInexistenteParaAdmin() {
        when(articleRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish("nao-existe"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete("nao-existe"))
                .isInstanceOf(NotFoundException.class);
        verify(articleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("tag ja existente e reaproveitada em vez de duplicada")
    void reaproveitaTagExistente() {
        Tag existente = Tag.of("RPG");
        when(tagRepository.findBySlug("rpg")).thenReturn(Optional.of(existente));

        Article criado = service.create(new ArticleRequest(
                "Elden Ring", "resumo", "texto", null, "Elden Ring", null, List.of("RPG")));

        assertThat(criado.getTags()).containsExactly(existente);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("tag nova e criada com slug proprio")
    void criaTagNova() {
        Article criado = service.create(new ArticleRequest(
                "Hollow Knight", "resumo", "texto", null, null, null, List.of("Souls-like")));

        assertThat(criado.getTags()).hasSize(1);
        assertThat(criado.getTags().iterator().next().getSlug()).isEqualTo("souls-like");
    }

    private ArticleRequest requisicao(String titulo, String markdown) {
        return new ArticleRequest(titulo, "Um resumo.", markdown, null, null, null, List.of());
    }

    private Article artigoSalvo(String slug, ArticleStatus status) {
        Article artigo = new Article();
        artigo.setId("id-1");
        artigo.setSlug(slug);
        artigo.setTitle("Elden Ring");
        artigo.setSummary("resumo");
        artigo.setContentMarkdown("texto");
        artigo.setContentHtml("<p>texto</p>");
        artigo.setStatus(status);
        return artigo;
    }
}
