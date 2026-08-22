package com.arthur.gitgud.image;

import com.arthur.gitgud.common.exception.BusinessException;
import com.arthur.gitgud.config.GitgudProperties;
import com.arthur.gitgud.support.Fixtures;
import com.arthur.gitgud.support.RelogioDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageServiceTest {

    private static final Instant AGOSTO_DE_2026 = Instant.parse("2026-08-21T12:00:00Z");

    @TempDir
    private Path pasta;

    private ImageService service;

    @BeforeEach
    void preparar() {
        service = criarService(DataSize.ofMegabytes(5), 1600);
    }

    @Test
    @DisplayName("converte PNG para WebP e devolve a URL publica")
    void convertePngParaWebP() throws Exception {
        var armazenada = service.armazenar(png("captura.png", 800, 600));

        assertThat(armazenada.url()).matches("/uploads/2026/08/[0-9a-f]{32}\\.webp");
        assertThat(arquivoDe(armazenada)).exists();
        assertThat(assinatura(arquivoDe(armazenada))).isEqualTo("WEBP");
    }

    @Test
    @DisplayName("print gigante e reduzido ate a largura maxima")
    void reduzImagemLarga() throws Exception {
        var armazenada = service.armazenar(png("print-4k.png", 3840, 2160));

        assertThat(armazenada.largura()).isEqualTo(1600);
        // A proporcao precisa ser mantida: 3840x2160 e 16:9.
        assertThat(armazenada.altura()).isEqualTo(900);
    }

    @Test
    @DisplayName("imagem menor que o limite nao e esticada")
    void naoAumentaImagemPequena() throws Exception {
        var armazenada = service.armazenar(png("pequena.png", 400, 300));

        assertThat(armazenada.largura()).isEqualTo(400);
        assertThat(armazenada.altura()).isEqualTo(300);
    }

    @Test
    @DisplayName("GIF continua GIF, para nao perder a animacao")
    void gifNaoEConvertido() throws Exception {
        var armazenada = service.armazenar(gif("jogada.gif"));

        // Converter para WebP aqui manteria so o primeiro quadro — e num blog de
        // jogos o GIF animado e justamente o ponto.
        assertThat(armazenada.url()).endsWith(".gif");
        assertThat(assinatura(arquivoDe(armazenada))).isEqualTo("GIF8");
    }

    @Test
    @DisplayName("arquivo que so finge ser imagem e recusado")
    void recusaArquivoQueNaoEImagem() {
        var disfarcado = new MockMultipartFile(
                "arquivo", "virus.png", "image/png", "isto aqui e texto".getBytes(StandardCharsets.UTF_8));

        // A checagem e pelos bytes do arquivo: extensao e content-type sao
        // informados por quem envia e mentem.
        assertThatThrownBy(() -> service.armazenar(disfarcado))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("imagem");
    }

    @Test
    @DisplayName("arquivo acima do limite e recusado antes de ser processado")
    void recusaArquivoGrande() {
        service = criarService(DataSize.ofKilobytes(10), 1600);

        assertThatThrownBy(() -> service.armazenar(png("enorme.png", 2000, 2000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("grande");
    }

    @Test
    @DisplayName("o nome do arquivo e gerado pelo servidor, nunca o enviado")
    void ignoraNomeEnviado() throws Exception {
        var armazenada = service.armazenar(png("../../../etc/senhas.png", 100, 100));

        // Confiar no nome enviado e o caminho classico para escrever fora da
        // pasta de uploads.
        assertThat(armazenada.url()).doesNotContain("..").doesNotContain("senhas");
        assertThat(arquivoDe(armazenada).normalize()).startsWith(pasta.normalize());
    }

    @Test
    @DisplayName("imagem com transparencia nao quebra a conversao")
    void aceitaTransparencia() throws Exception {
        var comAlfa = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        var armazenada = service.armazenar(comoPng("logo.png", comAlfa));

        assertThat(arquivoDe(armazenada)).exists();
    }

    @Test
    @DisplayName("arquivo vazio e recusado")
    void recusaArquivoVazio() {
        var vazio = new MockMultipartFile("arquivo", "nada.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.armazenar(vazio)).isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------ apoio

    private ImageService criarService(DataSize tamanhoMaximo, int larguraMaxima) {
        var propriedades = new GitgudProperties(
                Fixtures.propriedades().admin(),
                Fixtures.propriedades().jwt(),
                Fixtures.propriedades().cors(),
                Fixtures.propriedades().login(),
                new GitgudProperties.Uploads(pasta, tamanhoMaximo, larguraMaxima));

        return new ImageService(propriedades, new RelogioDeTeste(AGOSTO_DE_2026));
    }

    private Path arquivoDe(ImagemArmazenada armazenada) {
        return pasta.resolve(armazenada.url().replace("/uploads/", ""));
    }

    private String assinatura(Path arquivo) throws Exception {
        byte[] bytes = Files.readAllBytes(arquivo);
        // WebP: "RIFF" nos bytes 0-3 e "WEBP" nos 8-11. GIF: "GIF8" no inicio.
        return new String(bytes, 0, 4).equals("RIFF") ? new String(bytes, 8, 12 - 8) : new String(bytes, 0, 4);
    }

    private MockMultipartFile png(String nome, int largura, int altura) throws Exception {
        var imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        var g = imagem.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, largura, altura);
        g.dispose();
        return comoPng(nome, imagem);
    }

    private MockMultipartFile comoPng(String nome, BufferedImage imagem) throws Exception {
        var saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return new MockMultipartFile("arquivo", nome, "image/png", saida.toByteArray());
    }

    private MockMultipartFile gif(String nome) throws Exception {
        var imagem = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        var saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "gif", saida);
        return new MockMultipartFile("arquivo", nome, "image/gif", saida.toByteArray());
    }
}
