package com.arthur.gitgud.auth.security;

import com.arthur.gitgud.auth.service.TokenService;
import com.arthur.gitgud.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Traduz o {@code Authorization: Bearer} em um usuario autenticado no contexto. */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token != null) {
            String email = tokenService.validateToken(token);
            if (email != null) {
                // O papel vem do banco, nao do token: revogar o acesso de alguem
                // nao pode depender de esperar um JWT ja emitido expirar.
                userRepository.findByEmail(email).ifPresent(usuario -> {
                    var autoridades = List.of(
                            new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
                    // O principal e o email, nao a entidade: o hash da senha nao
                    // tem por que circular dentro do contexto de seguranca.
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                            usuario.getEmail(), null, autoridades);
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return null;
        }
        String token = cabecalho.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
