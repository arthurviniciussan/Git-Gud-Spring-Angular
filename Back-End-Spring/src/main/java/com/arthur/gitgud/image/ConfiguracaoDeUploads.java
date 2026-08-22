package com.arthur.gitgud.image;

import com.arthur.gitgud.config.GitgudProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve as imagens enviadas.
 *
 * <p>Em producao um proxy na frente pode servir a pasta direto, sem passar pela
 * aplicacao — mas ter isto aqui faz o upload funcionar em desenvolvimento sem
 * montar nada extra.
 */
@Configuration
public class ConfiguracaoDeUploads implements WebMvcConfigurer {

    private final GitgudProperties properties;

    public ConfiguracaoDeUploads(GitgudProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        // A barra final e obrigatoria: sem ela o Spring trata o ultimo trecho
        // como prefixo de arquivo em vez de pasta.
        String pasta = properties.uploads().dir().toAbsolutePath().normalize() + "/";

        registro.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + pasta)
                // Nome de arquivo com uuid nunca e reaproveitado: o conteudo de
                // uma URL jamais muda, entao da para cachear por bastante tempo.
                .setCachePeriod(60 * 60 * 24 * 30);
    }
}
