package com.arthur.gitgud.user.domain;

/**
 * Papel do usuario.
 *
 * <p>Hoje so existe {@code ADMIN}: o blog tem um autor unico e visitantes nao
 * criam conta. O enum existe para que a autorizacao seja explicita no codigo em
 * vez de assumida.
 */
public enum Role {
    ADMIN
}
