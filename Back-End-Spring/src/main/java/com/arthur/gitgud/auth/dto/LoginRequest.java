package com.arthur.gitgud.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "e obrigatorio")
        @Email(message = "formato invalido")
        String email,

        @NotBlank(message = "e obrigatoria")
        String password) {
}
