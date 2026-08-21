package com.arthur.gitgud.support;

import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.user.domain.Role;
import com.arthur.gitgud.user.domain.User;

import java.time.Duration;
import java.util.List;

/** Objetos de apoio compartilhados pelos testes. */
public final class Fixtures {

    /** Senha em texto que corresponde a {@link #HASH_VALIDO}. */
    public static final String SENHA_EM_TEXTO = "SenhaDeTeste123!";

    /** Hash BCrypt de {@link #SENHA_EM_TEXTO}, custo 10. */
    public static final String HASH_VALIDO = "$2a$10$4UwnIMc0fjUQRma/FaAz8ewVBdzsZgW0rBI2BJaWronVOv3nWuLQm";

    public static final String SEGREDO_JWT = "segredo-de-teste-com-bem-mais-de-32-caracteres";

    private Fixtures() {
    }

    public static User admin(String email) {
        User usuario = new User();
        usuario.setId("11111111-1111-1111-1111-111111111111");
        usuario.setName("Arthur");
        usuario.setEmail(email);
        usuario.setPassword(HASH_VALIDO);
        usuario.setRole(Role.ADMIN);
        return usuario;
    }

    public static GitgudProperties propriedades() {
        return new GitgudProperties(
                new GitgudProperties.Admin("admin@gitgud.dev", "Arthur", HASH_VALIDO),
                new GitgudProperties.Jwt(SEGREDO_JWT, "gitgud-api", Duration.ofHours(2)),
                new GitgudProperties.Cors(List.of("http://localhost:4200")),
                new GitgudProperties.Login(5, Duration.ofMinutes(15)));
    }
}
