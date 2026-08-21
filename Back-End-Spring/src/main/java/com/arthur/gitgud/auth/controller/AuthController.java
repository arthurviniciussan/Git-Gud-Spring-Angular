package com.arthur.gitgud.auth.controller;

import com.arthur.gitgud.auth.dto.LoginRequest;
import com.arthur.gitgud.auth.dto.LoginResponse;
import com.arthur.gitgud.auth.dto.SessionResponse;
import com.arthur.gitgud.auth.security.LoginRateLimiter;
import com.arthur.gitgud.auth.service.TokenService;
import com.arthur.gitgud.common.exception.NotFoundException;
import com.arthur.gitgud.user.domain.User;
import com.arthur.gitgud.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrada do painel de admin.
 *
 * <p>Nao existe cadastro. O {@code POST /auth/register} que existia aqui era
 * publico — qualquer pessoa com a URL criava uma conta no blog. O unico usuario
 * agora nasce do {@link com.arthur.gitgud.auth.config.AdminSeeder}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          TokenService tokenService,
                          LoginRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Autentica e devolve o token.
     *
     * <p>A comparacao de senha nao acontece mais aqui: quem faz e o
     * {@code AuthenticationManager}. E o que garante que email inexistente e
     * senha errada produzam a <b>mesma</b> resposta — o Spring Security embrulha
     * {@code UsernameNotFoundException} em {@code BadCredentialsException}, e o
     * {@code RestExceptionHandler} devolve 401 com mensagem unica.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao, HttpServletRequest http) {
        String origem = http.getRemoteAddr();
        rateLimiter.checkNotBlocked(origem);

        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            requisicao.email(), requisicao.password()));
        } catch (AuthenticationException e) {
            rateLimiter.recordFailure(origem);
            throw e;
        }

        rateLimiter.reset(origem);

        User usuario = buscarPorEmail(autenticacao.getName());
        return new LoginResponse(
                usuario.getName(),
                usuario.getEmail(),
                usuario.getRole().name(),
                tokenService.generateToken(usuario));
    }

    /** Usado pelo frontend ao abrir o painel, para saber se o token guardado ainda vale. */
    @GetMapping("/session")
    public SessionResponse session(Authentication autenticacao) {
        User usuario = buscarPorEmail(autenticacao.getName());
        return new SessionResponse(usuario.getName(), usuario.getEmail(), usuario.getRole().name());
    }

    private User buscarPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
    }
}
