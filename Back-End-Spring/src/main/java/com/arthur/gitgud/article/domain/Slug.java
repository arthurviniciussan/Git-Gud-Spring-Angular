package com.arthur.gitgud.article.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * O endereco publico de um artigo: {@code /artigo/elden-ring-e-dificil}.
 *
 * <p>E derivado do titulo, mas nao muda quando o titulo muda: link publicado e
 * link que precisa continuar funcionando.
 */
public record Slug(String value) {

    /** Cabe na coluna {@code slug VARCHAR(160)}, com folga para o sufixo. */
    public static final int TAMANHO_MAXIMO = 160;

    public Slug {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug nao pode ser vazio.");
        }
    }

    public static Slug of(String titulo) {
        String slug = truncar(normalizar(titulo));

        if (slug.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nao foi possivel gerar um endereco a partir do titulo: " + titulo);
        }
        return new Slug(slug);
    }

    /**
     * Desambigua titulos repetidos: {@code elden-ring-2}, {@code elden-ring-3}.
     *
     * <p>O sufixo entra dentro do tamanho maximo, encurtando a base se preciso —
     * do contrario o slug estouraria a coluna justamente no caso de titulo longo
     * e repetido.
     */
    public Slug comSufixo(int numero) {
        String sufixo = "-" + numero;
        String base = value.length() + sufixo.length() > TAMANHO_MAXIMO
                ? aparar(value.substring(0, TAMANHO_MAXIMO - sufixo.length()))
                : value;

        return new Slug(base + sufixo);
    }

    private static String normalizar(String titulo) {
        if (titulo == null) {
            return "";
        }
        // NFD separa a letra do acento ("á" -> "a" + "´"); o replace seguinte
        // descarta os acentos, deixando o ASCII por baixo.
        String semAcento = Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return aparar(semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-"));
    }

    /** Remove hifens sobrando nas pontas. */
    private static String aparar(String texto) {
        return texto.replaceAll("^-+|-+$", "");
    }

    private static String truncar(String slug) {
        return slug.length() <= TAMANHO_MAXIMO
                ? slug
                : aparar(slug.substring(0, TAMANHO_MAXIMO));
    }

    @Override
    public String toString() {
        return value;
    }
}
