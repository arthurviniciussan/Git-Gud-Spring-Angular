package com.arthur.gitgud.auth.dto;

/** Resposta do login: quem entrou e o token que autoriza as chamadas seguintes. */
public record LoginResponse(String name, String email, String role, String token) {
}
