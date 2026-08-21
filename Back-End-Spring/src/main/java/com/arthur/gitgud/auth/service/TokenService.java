package com.arthur.gitgud.auth.service;

import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.user.domain.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TokenService {

    private final Algorithm algorithm;
    private final GitgudProperties.Jwt properties;
    private final Clock clock;

    public TokenService(GitgudProperties properties, Clock clock) {
        this.properties = properties.jwt();
        this.algorithm = Algorithm.HMAC256(this.properties.secret());
        this.clock = clock;
    }

    public String generateToken(User user) {
        try {
            Instant agora = clock.instant();
            return JWT.create()
                    .withIssuer(properties.issuer())
                    .withSubject(user.getEmail())
                    .withClaim("name", user.getName())
                    .withClaim("role", user.getRole().name())
                    .withIssuedAt(agora)
                    // Antes isto era LocalDateTime.now().toInstant(ZoneOffset.of("-03:00")),
                    // que assumia que o servidor estava em -03:00. Num servidor
                    // em UTC o token vivia 5h em vez das 2h pretendidas.
                    .withExpiresAt(agora.plus(properties.expiration()))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new IllegalStateException("Falha ao gerar o token", e);
        }
    }

    /**
     * Devolve o email do dono do token, ou {@code null} se ele for invalido,
     * expirado ou de outro emissor.
     */
    public String validateToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
