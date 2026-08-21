package com.arthur.gitgud.auth.controller;

import com.arthur.gitgud.auth.security.LoginRateLimiter;
import com.arthur.gitgud.auth.security.SecurityConfig;
import com.arthur.gitgud.auth.security.SecurityFilter;
import com.arthur.gitgud.auth.service.TokenService;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.user.domain.User;
import com.arthur.gitgud.user.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de autenticacao, sem banco.
 *
 * <p>Cobre o que este PR promete: nao ha cadastro, resposta de erro nao vaza
 * quais emails existem, e forca bruta esbarra num limite.
 */
@WebMvcTest(AuthController.class)
// A fatia @WebMvcTest nao ativa o @ConfigurationPropertiesScan da aplicacao.
@EnableConfigurationProperties(GitgudProperties.class)
@Import({SecurityConfig.class, SecurityFilter.class, TokenService.class, LoginRateLimiter.class})
// Propriedades reais em vez de beans falsos: assim o teste tambem prova que os
// nomes usados no application.yaml batem com o que o GitgudProperties espera.
@TestPropertySource(properties = {
        "gitgud.admin.email=admin@gitgud.dev",
        "gitgud.admin.name=Arthur",
        "gitgud.admin.password-hash=" + Fixtures.HASH_VALIDO,
        "gitgud.jwt.secret=" + Fixtures.SEGREDO_JWT,
        "gitgud.jwt.issuer=gitgud-api",
        "gitgud.jwt.expiration=2h",
        "gitgud.cors.allowed-origins=http://localhost:4200",
        "gitgud.login.max-attempts=5",
        "gitgud.login.lock-duration=15m"
})
class AuthControllerTest {

    private static final String EMAIL = "admin@gitgud.dev";
    /** IP que o MockMvc usa em toda requisicao. */
    private static final String IP_DO_MOCKMVC = "127.0.0.1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private final User admin = Fixtures.admin(EMAIL);

    @BeforeEach
    void preparar() {
        // O limitador e singleton no contexto: sem isto, o teste de forca bruta
        // deixaria o IP bloqueado para os testes seguintes.
        rateLimiter.reset(IP_DO_MOCKMVC);
    }

    @Test
    @DisplayName("login valido devolve token e o papel do usuario")
    void loginValido() throws Exception {
        autenticacaoAceita();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        mvc.perform(login(EMAIL, "SenhaDeTeste123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("Arthur"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("senha errada e email inexistente respondem exatamente a mesma coisa")
    void respostasIndistinguiveis() throws Exception {
        autenticacaoRecusada();

        MvcResult senhaErrada = mvc.perform(login(EMAIL, "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        rateLimiter.reset(IP_DO_MOCKMVC);

        MvcResult emailInexistente = mvc.perform(login("ninguem@gitgud.dev", "qualquer-senha"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Se as duas respostas diferissem, daria para descobrir quais emails
        // existem so tentando logar.
        assertThat(mensagemDe(senhaErrada)).isEqualTo(mensagemDe(emailInexistente));
        assertThat(mensagemDe(senhaErrada)).isEqualTo("Credenciais invalidas.");
    }

    @Test
    @DisplayName("bloqueia a partir da sexta tentativa seguida")
    void limitaForcaBruta() throws Exception {
        autenticacaoRecusada();

        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            mvc.perform(login(EMAIL, "errada")).andExpect(status().isUnauthorized());
        }

        mvc.perform(login(EMAIL, "errada")).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("o cadastro publico nao existe mais")
    void cadastroFoiRemovido() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        String token = tokenService.generateToken(admin);

        // Autenticado para provar que e 404 (rota inexistente) e nao 401.
        mvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"invasor","email":"invasor@x.com","password":"12345678"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cadastro sem token nem chega a existir: responde 401")
    void cadastroAnonimoNaoPassa() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"invasor","email":"invasor@x.com","password":"12345678"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("sessao sem token responde 401 em JSON, no formato padrao de erro")
    void sessaoSemToken() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticacao necessaria."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("sessao com token valido diz quem esta logado, sem devolver token novo")
    void sessaoComToken() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        mvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + tokenService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("token adulterado nao autentica")
    void tokenAdulteradoNaoAutentica() throws Exception {
        mvc.perform(get("/api/auth/session").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("email fora de formato para na validacao, antes de qualquer tentativa")
    void validaFormatoDoEmail() throws Exception {
        mvc.perform(login("nao-e-email", "SenhaDeTeste123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: formato invalido"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String email, String senha) throws Exception {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginBody(email, senha)));
    }

    private void autenticacaoAceita() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of()));
    }

    /**
     * O Spring Security embrulha {@code UsernameNotFoundException} em
     * {@code BadCredentialsException}, entao email inexistente chega aqui igual a
     * senha errada. O mock reproduz esse comportamento.
     */
    private void autenticacaoRecusada() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
    }

    private String mensagemDe(MvcResult resultado) throws Exception {
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("message").asText();
    }

    private record LoginBody(String email, String password) {
    }
}
