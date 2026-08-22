package com.arthur.gitgud.image;

import com.arthur.gitgud.auth.security.LoginRateLimiter;
import com.arthur.gitgud.auth.security.SecurityConfig;
import com.arthur.gitgud.auth.security.SecurityFilter;
import com.arthur.gitgud.auth.service.TokenService;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminImageController.class)
@EnableConfigurationProperties(GitgudProperties.class)
@Import({SecurityConfig.class, SecurityFilter.class, TokenService.class, LoginRateLimiter.class})
@TestPropertySource(properties = {
        "gitgud.admin.email=admin@gitgud.dev",
        "gitgud.admin.name=Arthur",
        "gitgud.admin.password-hash=" + Fixtures.HASH_VALIDO,
        "gitgud.jwt.secret=" + Fixtures.SEGREDO_JWT,
        "gitgud.jwt.issuer=gitgud-api",
        "gitgud.jwt.expiration=2h",
        "gitgud.cors.allowed-origins=http://localhost:4200",
        "gitgud.login.max-attempts=5",
        "gitgud.login.lock-duration=15m",
        "gitgud.uploads.dir=dados/uploads",
        "gitgud.uploads.max-size=5MB",
        "gitgud.uploads.max-width=1600"
})
class AdminImageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void preparar() {
        var admin = Fixtures.admin("admin@gitgud.dev");
        when(userRepository.findByEmail("admin@gitgud.dev")).thenReturn(Optional.of(admin));
        token = tokenService.generateToken(admin);
    }

    private MockMultipartFile arquivo() {
        return new MockMultipartFile(
                "arquivo", "capa.png", "image/png", "conteudo".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("admin autenticado sobe a imagem e recebe a URL publica")
    void adminSobeImagem() throws Exception {
        when(imageService.armazenar(any()))
                .thenReturn(new ImagemArmazenada("/uploads/2026/08/abc.webp", 1600, 900, 120_000));

        mvc.perform(multipart("/api/admin/images").file(arquivo())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/2026/08/abc.webp"))
                .andExpect(jsonPath("$.largura").value(1600));
    }

    @Test
    @DisplayName("upload sem token responde 401 e nao chega no servico")
    void uploadSemTokenNaoPassa() throws Exception {
        mvc.perform(multipart("/api/admin/images").file(arquivo()))
                .andExpect(status().isUnauthorized());

        // O arquivo nao pode nem ser processado por quem nao esta autenticado.
        verify(imageService, never()).armazenar(any());
    }
}
