package com.arthur.gitgud.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guarda barata contra o Flyway deixar de rodar.
 *
 * <p>Existe por causa de uma falha real: com apenas {@code flyway-core} no
 * classpath, a biblioteca entra mas ninguem a executa — no Spring Boot 4 o
 * {@code FlywayAutoConfiguration} mora no modulo {@code spring-boot-flyway}.
 * O sintoma era distante da causa: a aplicacao subia, o Hibernate validava
 * contra um banco vazio e morria com "missing table [users]".
 *
 * <p>Sem Docker, o teste de integracao nao roda; estas assercoes rodam em
 * qualquer maquina e falham em milissegundos se a dependencia sumir.
 */
class MigrationsTest {

    @Test
    @DisplayName("a auto-configuracao do Flyway esta no classpath")
    void autoConfiguracaoDoFlywayPresente() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"))
                .as("falta a dependencia org.springframework.boot:spring-boot-flyway; "
                        + "sem ela as migrations nunca rodam")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("as migrations estao onde o Flyway procura")
    void migrationsNoLugarCerto() {
        assertThat(getClass().getResource("/db/migration/V1__baseline.sql")).isNotNull();
        assertThat(getClass().getResource("/db/migration/V2__add_user_role.sql")).isNotNull();
    }
}
