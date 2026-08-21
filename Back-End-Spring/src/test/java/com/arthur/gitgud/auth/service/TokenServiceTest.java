package com.arthur.gitgud.auth.service;

import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.support.RelogioDeTeste;
import com.auth0.jwt.JWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-21T12:00:00Z");

    @Test
    @DisplayName("o token carrega email, nome e papel")
    void tokenCarregaOsDados() {
        String token = servico(AGORA).generateToken(Fixtures.admin("admin@gitgud.dev"));

        var decodificado = JWT.decode(token);
        assertThat(decodificado.getSubject()).isEqualTo("admin@gitgud.dev");
        assertThat(decodificado.getClaim("name").asString()).isEqualTo("Arthur");
        assertThat(decodificado.getClaim("role").asString()).isEqualTo("ADMIN");
        assertThat(decodificado.getIssuer()).isEqualTo("gitgud-api");
    }

    @Test
    @DisplayName("expira exatamente na duracao configurada, independente do fuso do servidor")
    void expiraNaDuracaoConfigurada() {
        String token = servico(AGORA).generateToken(Fixtures.admin("admin@gitgud.dev"));

        // Regressao: a versao antiga usava ZoneOffset.of("-03:00") fixo, o que
        // dava 5h de validade num servidor em UTC em vez das 2h configuradas.
        assertThat(JWT.decode(token).getExpiresAtAsInstant())
                .isEqualTo(AGORA.plus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("valida o proprio token e devolve o email")
    void validaTokenValido() {
        // A emissao usa o relogio injetado, mas a verificacao usa o relogio do
        // sistema — que e o comportamento certo em producao. Por isso o token
        // aqui e emitido "agora", e nao numa data fixa.
        TokenService servico = servico(Instant.now());
        String token = servico.generateToken(Fixtures.admin("admin@gitgud.dev"));

        assertThat(servico.validateToken(token)).isEqualTo("admin@gitgud.dev");
    }

    @Test
    @DisplayName("recusa token expirado")
    void recusaTokenExpirado() {
        // Emitido tres horas atras, com validade de duas: ja nasceu vencido para
        // o relogio do sistema.
        TokenService servico = servico(Instant.now().minus(Duration.ofHours(3)));
        String token = servico.generateToken(Fixtures.admin("admin@gitgud.dev"));

        assertThat(servico.validateToken(token)).isNull();
    }

    @Test
    @DisplayName("recusa token assinado com outro segredo")
    void recusaAssinaturaDeOutroSegredo() {
        var outrasPropriedades = new com.arthur.gitgud.config.GitgudProperties(
                Fixtures.propriedades().admin(),
                new com.arthur.gitgud.config.GitgudProperties.Jwt(
                        "outro-segredo-igualmente-longo-para-hs256", "gitgud-api", Duration.ofHours(2)),
                Fixtures.propriedades().cors(),
                Fixtures.propriedades().login());

        String tokenDeOutraApp = new TokenService(outrasPropriedades, new RelogioDeTeste(Instant.now()))
                .generateToken(Fixtures.admin("admin@gitgud.dev"));

        // Token dentro da validade: o que o derruba e a assinatura, nao o prazo.
        assertThat(servico(Instant.now()).validateToken(tokenDeOutraApp)).isNull();
    }

    @Test
    @DisplayName("recusa lixo no lugar do token")
    void recusaTokenMalformado() {
        assertThat(servico(Instant.now()).validateToken("nao-e-um-jwt")).isNull();
    }

    private TokenService servico(Instant agora) {
        return new TokenService(Fixtures.propriedades(), new RelogioDeTeste(agora));
    }
}
