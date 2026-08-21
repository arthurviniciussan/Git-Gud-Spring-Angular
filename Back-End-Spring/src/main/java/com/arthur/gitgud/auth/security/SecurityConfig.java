package com.arthur.gitgud.auth.security;

import com.arthur.gitgud.common.dto.ErrorResponse;
import com.arthur.gitgud.config.GitgudProperties;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Autenticacao do blog.
 *
 * <p><b>Um autor, nenhum visitante cadastrado.</b> Nao existe endpoint de
 * cadastro: o unico usuario e semeado por variavel de ambiente
 * ({@link com.arthur.gitgud.auth.config.AdminSeeder}). Visitantes leem o blog
 * sem conta; escrever exige {@code ROLE_ADMIN}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final GitgudProperties properties;

    public SecurityConfig(SecurityFilter securityFilter, GitgudProperties properties) {
        this.securityFilter = securityFilter;
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Sem cookie de sessao nao ha o que um site de terceiros possa
                // fazer o navegador enviar sozinho: o token vai num header que
                // so o nosso frontend anexa.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // O blog em si. Ler nao exige conta — e o ponto do produto.
                        // Restrito a GET de proposito: sem o metodo, um POST em
                        // /api/articles tambem passaria pelo permitAll.
                        .requestMatchers(HttpMethod.GET,
                                "/api/articles", "/api/articles/**", "/api/tags").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((requisicao, resposta, excecao) ->
                                escreverErro(mapper, resposta, HttpStatus.UNAUTHORIZED,
                                        "Autenticacao necessaria."))
                        .accessDeniedHandler((requisicao, resposta, excecao) ->
                                escreverErro(mapper, resposta, HttpStatus.FORBIDDEN,
                                        "Acesso negado.")))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Erro nascido no filtro sai no mesmo formato do resto da API.
     *
     * <p>Duas razoes para escrever isto na mao: o padrao do Spring Security
     * devolveria {@code WWW-Authenticate}, o que faz o navegador abrir a caixa de
     * login nativa — inutil numa SPA; e excecoes do filtro nao passam pelo
     * {@code RestControllerAdvice}, entao o formato precisa ser reproduzido aqui.
     */
    private static void escreverErro(ObjectMapper mapper, HttpServletResponse resposta,
                                     HttpStatus status, String mensagem) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(resposta.getWriter(), ErrorResponse.of(mensagem));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        // Origens vem da configuracao: em producao o dominio real nao e localhost.
        configuracao.setAllowedOrigins(properties.cors().allowedOrigins());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
