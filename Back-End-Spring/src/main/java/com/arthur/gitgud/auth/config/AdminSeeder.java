package com.arthur.gitgud.auth.config;

import com.arthur.gitgud.auth.security.CustomUserDetailsService;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.user.domain.Role;
import com.arthur.gitgud.user.domain.User;
import com.arthur.gitgud.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Cria (ou atualiza) o unico usuario do blog a cada subida.
 *
 * <p>Substitui o antigo {@code POST /auth/register}, que era publico. Trocar de
 * senha vira "troca a variavel de ambiente e reinicia" — sem SQL na mao e sem
 * endpoint de cadastro exposto.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    /** BCrypt: {@code $2a$}/{@code $2b$}/{@code $2y$}, custo de dois digitos, 53 caracteres de sal+hash. */
    private static final Pattern HASH_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final UserRepository userRepository;
    private final GitgudProperties.Admin propriedades;

    public AdminSeeder(UserRepository userRepository, GitgudProperties properties) {
        this.userRepository = userRepository;
        this.propriedades = properties.admin();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = propriedades.passwordHash().trim();

        // Colar a senha em texto no lugar do hash e o erro mais facil de cometer
        // aqui, e o mais silencioso: o login simplesmente nunca funcionaria.
        // Melhor a aplicacao nao subir.
        if (!HASH_BCRYPT.matcher(hash).matches()) {
            throw new IllegalStateException(
                    "GITGUD_ADMIN_PASSWORD_HASH nao e um hash BCrypt valido. "
                            + "Gere com: docker run --rm httpd:2.4-alpine "
                            + "htpasswd -nbBC 12 admin 'SUA_SENHA' | cut -d: -f2");
        }

        String email = CustomUserDetailsService.normalizarEmail(propriedades.email());

        User admin = userRepository.findByEmail(email).orElseGet(User::new);
        boolean novo = admin.getId() == null;

        admin.setEmail(email);
        admin.setName(propriedades.name());
        admin.setPassword(hash);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Admin {}: {}", novo ? "criado" : "atualizado", email);
    }
}
