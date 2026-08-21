package com.arthur.gitgud.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base dos testes que precisam de banco.
 *
 * <p>Sobe um MySQL 8.0 real — a mesma versao do {@code docker-compose}. Nao
 * usamos H2: as migrations sao SQL de MySQL e o {@code ddl-auto: validate} so
 * prova alguma coisa se conferir as entidades contra o banco de verdade.
 *
 * <p><b>Exige Docker.</b> O Testcontainers gerencia o container sozinho; nao e
 * preciso ter o {@code docker compose up} rodando.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegracaoComBancoTest {

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        return new MySQLContainer("mysql:8.0");
    }
}
