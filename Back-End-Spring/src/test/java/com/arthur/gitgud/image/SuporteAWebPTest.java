package com.arthur.gitgud.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda contra a lib de WebP sumir ou o binario nativo nao carregar.
 *
 * <p>O writer vem de uma dependencia com codigo nativo embutido. Se ele nao
 * carregar, o sintoma apareceria so no primeiro upload em producao — e como
 * "imagem nao pode ser salva", sem apontar a causa.
 */
class SuporteAWebPTest {

    @Test
    @DisplayName("o ImageIO sabe escrever WebP")
    void escritorDeWebPDisponivel() {
        assertThat(ImageIO.getWriterFormatNames())
                .as("falta a dependencia org.sejda.imageio:webp-imageio, ou o binario nativo nao carregou")
                .anyMatch(formato -> formato.equalsIgnoreCase("webp"));
    }

    @Test
    @DisplayName("escrever um WebP de verdade produz bytes com a assinatura RIFF/WEBP")
    void escreveWebPValido() throws Exception {
        BufferedImage imagem = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        var saida = new ByteArrayOutputStream();

        boolean escreveu = ImageIO.write(imagem, "webp", saida);

        assertThat(escreveu).isTrue();
        byte[] bytes = saida.toByteArray();
        assertThat(new String(Arrays.copyOfRange(bytes, 0, 4))).isEqualTo("RIFF");
        assertThat(new String(Arrays.copyOfRange(bytes, 8, 12))).isEqualTo("WEBP");
    }
}
