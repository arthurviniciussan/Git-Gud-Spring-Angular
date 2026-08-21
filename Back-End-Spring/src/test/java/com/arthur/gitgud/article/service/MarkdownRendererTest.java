package com.arthur.gitgud.article.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O Markdown vira HTML aqui, no servidor, e ja sai sanitizado.
 *
 * <p>Isso mantem uma unica fonte de sanitizacao: o frontend recebe HTML pronto
 * e so o injeta. Se a limpeza morasse no navegador, a renderizacao no servidor
 * (Etapa 5) precisaria repetir a mesma logica — e duas implementacoes divergem.
 */
class MarkdownRendererTest {

    private MarkdownRenderer renderer;

    @BeforeEach
    void preparar() {
        renderer = new MarkdownRenderer();
    }

    @Test
    @DisplayName("converte a formatacao basica")
    void converteFormatacaoBasica() {
        String html = renderer.toHtml("# Título\n\nUm **jogo** muito *bom*.");

        assertThat(html).contains("<h1>Título</h1>");
        assertThat(html).contains("<strong>jogo</strong>");
        assertThat(html).contains("<em>bom</em>");
    }

    @Test
    @DisplayName("converte listas, citacoes e blocos de codigo")
    void converteEstruturas() {
        String html = renderer.toHtml("""
                - um
                - dois

                > citação

                ```java
                int x = 1;
                ```
                """);

        assertThat(html).contains("<ul>").contains("<li>um</li>");
        assertThat(html).contains("<blockquote>");
        assertThat(html).contains("<code");
    }

    @Test
    @DisplayName("mantem imagem e link, que sao o basico de um artigo de review")
    void mantemImagemELink() {
        String html = renderer.toHtml(
                "![Capa](/uploads/2026/08/capa.webp)\n\n[site](https://exemplo.com)");

        assertThat(html).contains("<img").contains("src=\"/uploads/2026/08/capa.webp\"");
        assertThat(html).contains("<a href=\"https://exemplo.com\"");
    }

    @Test
    @DisplayName("remove script embutido no markdown")
    void removeScript() {
        String html = renderer.toHtml("Antes\n\n<script>alert('xss')</script>\n\nDepois");

        assertThat(html).doesNotContain("<script").doesNotContain("alert(");
        assertThat(html).contains("Antes").contains("Depois");
    }

    @Test
    @DisplayName("remove handler de evento em tag permitida")
    void removeHandlerDeEvento() {
        String html = renderer.toHtml("<img src=\"x\" onerror=\"alert('xss')\">");

        assertThat(html).doesNotContain("onerror").doesNotContain("alert(");
    }

    @Test
    @DisplayName("remove link com javascript: no href")
    void removeJavascriptNoHref() {
        String html = renderer.toHtml("[clique](javascript:alert('xss'))");

        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("remove iframe, que abriria espaco para clickjacking")
    void removeIframe() {
        String html = renderer.toHtml("<iframe src=\"https://evil.example\"></iframe>");

        assertThat(html).doesNotContain("<iframe");
    }

    @Test
    @DisplayName("texto vazio ou nulo vira HTML vazio, nao explode")
    void toleraVazio() {
        assertThat(renderer.toHtml(null)).isEmpty();
        assertThat(renderer.toHtml("   ")).isEmpty();
    }

    @Test
    @DisplayName("escapa HTML dentro de bloco de codigo em vez de executar")
    void escapaCodigo() {
        String html = renderer.toHtml("`<script>alert(1)</script>`");

        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).doesNotContain("<script>");
    }
}
