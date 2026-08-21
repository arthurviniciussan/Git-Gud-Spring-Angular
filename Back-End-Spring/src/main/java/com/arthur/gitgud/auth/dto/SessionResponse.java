package com.arthur.gitgud.auth.dto;

/**
 * Quem esta logado agora.
 *
 * <p>Deliberadamente sem o token: o frontend ja tem o dele, e reemitir um token
 * a cada consulta de sessao so aumentaria as chances de ele vazar em log.
 */
public record SessionResponse(String name, String email, String role) {
}
