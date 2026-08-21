package com.arthur.gitgud.article.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O slug e o endereco publico do artigo: /artigo/elden-ring-e-dificil.
 *
 * <p>Precisa ser estavel e seguro para URL — acento, pontuacao e espaco nao
 * podem vazar para o endereco.
 */
class SlugTest {

    @ParameterizedTest
    @DisplayName("deriva um endereco legivel a partir do titulo")
    @CsvSource({
            "'Elden Ring é difícil',            elden-ring-e-dificil",
            "'Hollow Knight: Silksong',         hollow-knight-silksong",
            "'  Espaços   demais  ',            espacos-demais",
            "'Ação, Aventura & RPG',            acao-aventura-rpg",
            "'MAIÚSCULAS VIRAM minúsculas',     maiusculas-viram-minusculas",
            "'Pokémon Violeta (análise)',       pokemon-violeta-analise",
            "'Trilha---sonora',                 trilha-sonora",
            "'2077 é o ano',                    2077-e-o-ano",
    })
    void derivaDoTitulo(String titulo, String esperado) {
        assertThat(Slug.of(titulo).value()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("nao deixa hifen sobrando nas pontas")
    void semHifenNasPontas() {
        assertThat(Slug.of("!!! Começo e fim !!!").value()).isEqualTo("comeco-e-fim");
    }

    @Test
    @DisplayName("trunca titulo muito longo sem cortar no meio do hifen")
    void truncaTituloLongo() {
        String tituloLongo = "palavra ".repeat(40);

        String slug = Slug.of(tituloLongo).value();

        assertThat(slug).hasSizeLessThanOrEqualTo(Slug.TAMANHO_MAXIMO);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    @DisplayName("titulo sem nenhum caractere aproveitavel e recusado")
    void recusaTituloSemLetras() {
        // Um titulo so de simbolos geraria slug vazio — e uma URL vazia nao
        // aponta para lugar nenhum.
        assertThatThrownBy(() -> Slug.of("!!! ??? ..."))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dois artigos com o mesmo titulo recebem sufixo")
    void desambiguaComSufixo() {
        Slug original = Slug.of("Elden Ring é difícil");

        assertThat(original.comSufixo(2).value()).isEqualTo("elden-ring-e-dificil-2");
        assertThat(original.comSufixo(3).value()).isEqualTo("elden-ring-e-dificil-3");
    }

    @Test
    @DisplayName("o sufixo cabe dentro do tamanho maximo")
    void sufixoRespeitaTamanhoMaximo() {
        Slug longo = Slug.of("palavra ".repeat(40));

        assertThat(longo.comSufixo(12).value()).hasSizeLessThanOrEqualTo(Slug.TAMANHO_MAXIMO);
        assertThat(longo.comSufixo(12).value()).endsWith("-12");
    }
}
