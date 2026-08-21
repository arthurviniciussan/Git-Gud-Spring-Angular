package com.arthur.gitgud.auth.config;

import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.user.domain.Role;
import com.arthur.gitgud.user.domain.User;
import com.arthur.gitgud.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("cria o admin quando o banco esta vazio")
    void criaQuandoNaoExiste() {
        when(userRepository.findByEmail("admin@gitgud.dev")).thenReturn(Optional.empty());

        semear(propriedades("admin@gitgud.dev", "Arthur", Fixtures.HASH_VALIDO));

        User salvo = capturarSalvo();
        assertThat(salvo.getEmail()).isEqualTo("admin@gitgud.dev");
        assertThat(salvo.getName()).isEqualTo("Arthur");
        assertThat(salvo.getPassword()).isEqualTo(Fixtures.HASH_VALIDO);
        assertThat(salvo.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("atualiza o admin existente em vez de criar um segundo")
    void atualizaQuandoJaExiste() {
        User existente = Fixtures.admin("admin@gitgud.dev");
        existente.setPassword("$2a$10$hashAntigoQueSeraSubstituidoPeloNovoAoSubirAAAAAAAAAAAAA");
        when(userRepository.findByEmail("admin@gitgud.dev")).thenReturn(Optional.of(existente));

        semear(propriedades("admin@gitgud.dev", "Arthur Vinicius", Fixtures.HASH_VALIDO));

        User salvo = capturarSalvo();
        // Trocar de senha e trocar a variavel de ambiente e reiniciar.
        assertThat(salvo.getId()).isEqualTo(existente.getId());
        assertThat(salvo.getPassword()).isEqualTo(Fixtures.HASH_VALIDO);
        assertThat(salvo.getName()).isEqualTo("Arthur Vinicius");
    }

    @Test
    @DisplayName("normaliza o email para minusculas")
    void normalizaEmail() {
        when(userRepository.findByEmail("admin@gitgud.dev")).thenReturn(Optional.empty());

        semear(propriedades("  Admin@GitGud.DEV  ", "Arthur", Fixtures.HASH_VALIDO));

        assertThat(capturarSalvo().getEmail()).isEqualTo("admin@gitgud.dev");
    }

    @Test
    @DisplayName("nao sobe se a senha foi colada em texto no lugar do hash")
    void recusaSenhaEmTexto() {
        assertThatThrownBy(() -> semear(propriedades("admin@gitgud.dev", "Arthur", "MinhaSenhaForte123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BCrypt");
    }

    @Test
    @DisplayName("nao sobe com hash truncado")
    void recusaHashTruncado() {
        assertThatThrownBy(() -> semear(propriedades("admin@gitgud.dev", "Arthur", "$2a$10$curtoDemais")))
                .isInstanceOf(IllegalStateException.class);
    }

    private void semear(GitgudProperties propriedades) {
        new AdminSeeder(userRepository, propriedades).run(null);
    }

    private User capturarSalvo() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private GitgudProperties propriedades(String email, String nome, String hash) {
        return new GitgudProperties(
                new GitgudProperties.Admin(email, nome, hash),
                new GitgudProperties.Jwt(Fixtures.SEGREDO_JWT, "gitgud-api", Duration.ofHours(2)),
                new GitgudProperties.Cors(List.of("http://localhost:4200")),
                new GitgudProperties.Login(5, Duration.ofMinutes(15)));
    }
}
