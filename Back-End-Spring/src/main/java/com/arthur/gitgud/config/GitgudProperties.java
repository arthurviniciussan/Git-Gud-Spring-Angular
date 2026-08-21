package com.arthur.gitgud.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Configuracao da aplicacao, validada na subida.
 *
 * <p>Nada aqui tem valor padrao embutido: se uma propriedade obrigatoria faltar,
 * a aplicacao <b>nao sobe</b> e a mensagem diz qual e. E o oposto do que existia
 * antes, quando um segredo de JWT fraco vinha como default e ninguem percebia
 * que estava rodando com ele em producao.
 */
@Validated
@ConfigurationProperties("gitgud")
public record GitgudProperties(
        @Valid @NotNull Admin admin,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Login login) {

    /**
     * O unico usuario do sistema.
     *
     * <p>A senha nunca aparece em texto: o que se configura e o hash BCrypt,
     * vindo de {@code GITGUD_ADMIN_PASSWORD_HASH}.
     */
    public record Admin(
            @NotBlank(message = "defina GITGUD_ADMIN_EMAIL") String email,
            @NotBlank(message = "defina GITGUD_ADMIN_NAME") String name,
            @NotBlank(message = "defina GITGUD_ADMIN_PASSWORD_HASH") String passwordHash) {
    }

    public record Jwt(
            /* HS256 com segredo curto e quebravel por forca bruta. */
            @NotBlank(message = "defina GITGUD_JWT_SECRET")
            @Size(min = 32, message = "precisa de ao menos 32 caracteres (HS256 exige 256 bits)")
            String secret,

            @NotBlank String issuer,
            @NotNull Duration expiration) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record Login(@Positive int maxAttempts, @NotNull Duration lockDuration) {
    }
}
