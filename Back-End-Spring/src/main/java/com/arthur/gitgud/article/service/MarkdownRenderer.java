package com.arthur.gitgud.article.service;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o Markdown do artigo em HTML seguro.
 *
 * <p><b>Por que no servidor.</b> O artigo e gravado em Markdown e servido ja
 * como HTML. Assim existe uma unica sanitizacao, e a renderizacao no servidor
 * (Etapa 5) nao precisa carregar um sanitizador de JavaScript.
 *
 * <p><b>Por que sanitizar mesmo sendo eu o unico autor.</b> Porque o custo e
 * baixo e o estrago seria grande: basta um trecho copiado de uma pagina
 * qualquer, com um {@code onerror} escondido, para o proprio blog servir o
 * ataque aos leitores. A regra e a mesma de sempre — o que vira HTML passa pela
 * lista de permissao.
 */
@Component
public class MarkdownRenderer {

    /**
     * Base ficticia usada so para validar URL relativa.
     *
     * <p>O Jsoup confere o protocolo de {@code img[src]} e {@code a[href]}. Sem
     * uma base, {@code /uploads/capa.webp} nao resolve para nenhum protocolo e e
     * descartada. Com a base, ela resolve, passa na checagem, e o
     * {@code preserveRelativeLinks} devolve o caminho relativo original — que e
     * o que precisa ir para o HTML, ja que a imagem e servida pela propria
     * aplicacao, em qualquer dominio onde ela esteja.
     */
    private static final String BASE_PARA_VALIDACAO = "https://gitgud.invalid/";

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist listaDePermissao;

    public MarkdownRenderer() {
        List<org.commonmark.Extension> extensoes = List.of(TablesExtension.create());

        this.parser = Parser.builder().extensions(extensoes).build();
        this.renderer = HtmlRenderer.builder().extensions(extensoes).build();
        this.listaDePermissao = safelist();
    }

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String bruto = renderer.render(parser.parse(markdown));

        // O Jsoup e quem decide o que sobrevive. O commonmark repassa HTML cru
        // do markdown sem julgar, entao a limpeza precisa vir depois dele.
        return Jsoup.clean(bruto, BASE_PARA_VALIDACAO, listaDePermissao, saidaSemIndentacao());
    }

    /**
     * O que um artigo de review precisa, e nada alem.
     *
     * <p>Fora da lista, entre outros: {@code <script>}, {@code <iframe>},
     * {@code <form>} e qualquer atributo {@code on*}. O Jsoup tambem descarta
     * sozinho URL com protocolo nao declarado — e o que derruba
     * {@code href="javascript:..."}.
     */
    private Safelist safelist() {
        return Safelist.relaxed()
                .addAttributes("img", "loading", "width", "height")
                .addAttributes("a", "rel", "target")
                .addAttributes("code", "class")
                .addAttributes("pre", "class")
                // Capas e imagens do artigo sao servidas pela propria aplicacao,
                // em caminho relativo — sem isto o Jsoup descartaria /uploads/...
                .preserveRelativeLinks(true);
    }

    /** Mantem o HTML compacto; a indentacao do Jsoup vira espaco visivel dentro de <pre>. */
    private Document.OutputSettings saidaSemIndentacao() {
        return new Document.OutputSettings().prettyPrint(false);
    }
}
