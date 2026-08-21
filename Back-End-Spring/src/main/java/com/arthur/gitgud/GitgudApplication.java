package com.arthur.gitgud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GitgudApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitgudApplication.class, args);
	}

	/**
	 * Relogio injetavel.
	 *
	 * <p>Expiracao de token e janela de bloqueio de login dependem do tempo. Com
	 * o relogio injetado, testar "o token expirou" ou "a janela passou" nao exige
	 * esperar de verdade.
	 */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
