package com.arthur.gitgud.auth;

import com.arthur.gitgud.auth.config.AdminSeeder;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.support.IntegracaoComBancoTest;
import com.arthur.gitgud.user.domain.Role;
import com.arthur.gitgud.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O caminho completo contra um MySQL real: migrations aplicadas, entidade
 * conferida pelo Hibernate, admin semeado e login de verdade.
 *
 * <p>O que so este teste pega: divergencia entre as migrations e a entidade
 * {@code User}. Com {@code ddl-auto: validate}, uma coluna com tamanho ou
 * nulidade diferente derruba a aplicacao na subida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegracaoComBancoTest.class)
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
class AuthFlowIntegrationTest {

    private static final String SENHA = Fixtures.SENHA_EM_TEXTO;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminSeeder adminSeeder;

    @Test
    @DisplayName("as migrations sobem e o admin nasce com papel ADMIN")
    void migrationsEAdminSemeado() {
        var admin = userRepository.findByEmail("admin@gitgud.dev").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPassword()).isEqualTo(Fixtures.HASH_VALIDO);
    }

    @Test
    @DisplayName("subir de novo atualiza o admin em vez de criar um segundo")
    void seederNaoDuplica() {
        // O seeder ja rodou na subida do contexto; rodar outra vez simula um
        // restart da aplicacao.
        adminSeeder.run(null);

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("login de ponta a ponta devolve um token utilizavel")
    void loginRealFunciona() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gitgud.dev","password":"%s"}""".formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("senha errada contra o banco real tambem devolve 401 generico")
    void senhaErradaNoBancoReal() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gitgud.dev","password":"NaoEhAMinhaSenha1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));
    }
}
