package com.arthur.gitgud.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relogio controlavel.
 *
 * <p>Existe para que testes de expiracao de token e de janela de bloqueio nao
 * precisem esperar o tempo real passar.
 */
public class RelogioDeTeste extends Clock {

    private Instant agora;
    private final ZoneId zona;

    public RelogioDeTeste(Instant inicio) {
        this(inicio, ZoneId.of("UTC"));
    }

    private RelogioDeTeste(Instant inicio, ZoneId zona) {
        this.agora = inicio;
        this.zona = zona;
    }

    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return new RelogioDeTeste(agora, zona);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
