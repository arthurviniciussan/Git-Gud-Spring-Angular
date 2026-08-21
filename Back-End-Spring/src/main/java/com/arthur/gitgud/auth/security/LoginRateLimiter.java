package com.arthur.gitgud.auth.security;

import com.arthur.gitgud.common.exception.TooManyRequestsException;
import com.arthur.gitgud.config.GitgudProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas de login por IP.
 *
 * <p>Com um usuario unico, a senha do admin e a unica fechadura do site: sem
 * limite, um atacante pode tentar senhas indefinidamente contra um email que ele
 * ja sabe qual e.
 *
 * <p><b>Limitacoes conhecidas.</b> O contador vive em memoria: some no restart e
 * nao e compartilhado entre instancias. Para um VPS com uma instancia isso
 * basta; se um dia houver mais de uma, isto precisa virar Redis ou bucket4j.
 */
@Component
public class LoginRateLimiter {

    /**
     * Acima disto, varremos as entradas expiradas. Sem essa poda o mapa cresce
     * sem limite sob um ataque distribuido — cada IP novo deixaria um registro.
     */
    private static final int LIMITE_PARA_PODA = 1_000;

    private final Map<String, Tentativas> porChave = new ConcurrentHashMap<>();
    private final int maxTentativas;
    private final Duration duracaoDoBloqueio;
    private final Clock clock;

    public LoginRateLimiter(GitgudProperties properties, Clock clock) {
        this.maxTentativas = properties.login().maxAttempts();
        this.duracaoDoBloqueio = properties.login().lockDuration();
        this.clock = clock;
    }

    /** Barra a tentativa se a chave ja estourou o limite dentro da janela. */
    public void checkNotBlocked(String chave) {
        Tentativas tentativas = porChave.get(chave);
        if (tentativas != null && estaBloqueada(tentativas)) {
            throw new TooManyRequestsException(
                    "Tentativas de login demais. Aguarde " + duracaoDoBloqueio.toMinutes() + " minutos.");
        }
    }

    public void recordFailure(String chave) {
        Instant agora = clock.instant();
        podarSeNecessario(agora);

        porChave.compute(chave, (ignorada, atual) -> {
            if (atual == null || expirou(atual, agora)) {
                return new Tentativas(1, agora);
            }
            return new Tentativas(atual.contagem() + 1, atual.primeiraEm());
        });
    }

    /** Login bem-sucedido zera o contador daquela origem. */
    public void reset(String chave) {
        porChave.remove(chave);
    }

    private boolean estaBloqueada(Tentativas tentativas) {
        return tentativas.contagem() >= maxTentativas && !expirou(tentativas, clock.instant());
    }

    private boolean expirou(Tentativas tentativas, Instant agora) {
        return agora.isAfter(tentativas.primeiraEm().plus(duracaoDoBloqueio));
    }

    private void podarSeNecessario(Instant agora) {
        if (porChave.size() > LIMITE_PARA_PODA) {
            porChave.values().removeIf(tentativas -> expirou(tentativas, agora));
        }
    }

    private record Tentativas(int contagem, Instant primeiraEm) {
    }
}
