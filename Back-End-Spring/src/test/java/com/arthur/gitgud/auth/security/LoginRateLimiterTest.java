package com.arthur.gitgud.auth.security;

import com.arthur.gitgud.common.exception.TooManyRequestsException;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.support.RelogioDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private static final String IP = "203.0.113.10";
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private RelogioDeTeste relogio;
    private LoginRateLimiter limitador;

    @BeforeEach
    void preparar() {
        relogio = new RelogioDeTeste(Instant.parse("2026-08-21T12:00:00Z"));
        limitador = new LoginRateLimiter(propriedades(5, BLOQUEIO), relogio);
    }

    @Test
    @DisplayName("deixa passar enquanto o limite nao foi atingido")
    void permiteAbaixoDoLimite() {
        for (int i = 0; i < 4; i++) {
            limitador.recordFailure(IP);
        }

        assertThatCode(() -> limitador.checkNotBlocked(IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloqueia a partir da quinta falha")
    void bloqueiaNoLimite() {
        for (int i = 0; i < 5; i++) {
            limitador.recordFailure(IP);
        }

        assertThatThrownBy(() -> limitador.checkNotBlocked(IP))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("15");
    }

    @Test
    @DisplayName("libera de novo quando a janela expira")
    void liberaDepoisDaJanela() {
        for (int i = 0; i < 5; i++) {
            limitador.recordFailure(IP);
        }

        relogio.avancar(BLOQUEIO.plusSeconds(1));

        assertThatCode(() -> limitador.checkNotBlocked(IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login bem-sucedido zera o contador")
    void resetZeraContador() {
        for (int i = 0; i < 4; i++) {
            limitador.recordFailure(IP);
        }
        limitador.reset(IP);
        limitador.recordFailure(IP);

        assertThatCode(() -> limitador.checkNotBlocked(IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloqueio de um IP nao afeta outro")
    void bloqueioEPorOrigem() {
        for (int i = 0; i < 5; i++) {
            limitador.recordFailure(IP);
        }

        assertThatCode(() -> limitador.checkNotBlocked("198.51.100.7")).doesNotThrowAnyException();
    }

    private GitgudProperties propriedades(int maxTentativas, Duration bloqueio) {
        return new GitgudProperties(
                new GitgudProperties.Admin("admin@gitgud.dev", "Admin", "hash"),
                new GitgudProperties.Jwt("um-segredo-de-teste-com-mais-de-32-caracteres", "gitgud-api",
                        Duration.ofHours(2)),
                new GitgudProperties.Cors(List.of("http://localhost:4200")),
                new GitgudProperties.Login(maxTentativas, bloqueio),
                Fixtures.propriedades().uploads());
    }
}
