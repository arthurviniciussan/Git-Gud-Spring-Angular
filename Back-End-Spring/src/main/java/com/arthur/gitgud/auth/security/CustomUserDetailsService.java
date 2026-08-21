package com.arthur.gitgud.auth.security;

import com.arthur.gitgud.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Ponte entre a tabela {@code users} e o Spring Security.
 *
 * <p>Antes esta classe existia mas nunca era chamada — o filtro carregava o
 * usuario direto do repositorio. Agora ela e o que o {@code AuthenticationManager}
 * usa no login, o que traz de graca o comportamento de esconder
 * {@code UsernameNotFoundException} dentro de {@code BadCredentialsException}:
 * email inexistente e senha errada ficam indistinguiveis para quem chama.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalizarEmail(username))
                .map(usuario -> org.springframework.security.core.userdetails.User
                        .withUsername(usuario.getEmail())
                        .password(usuario.getPassword())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }

    /** Email nao diferencia maiuscula de minuscula: {@code A@x.com} e {@code a@x.com} sao o mesmo login. */
    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
