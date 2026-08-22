package com.arthur.gitgud.image;

import com.arthur.gitgud.common.exception.BusinessException;
import com.arthur.gitgud.config.GitgudProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Guarda as imagens dos artigos em disco.
 *
 * <p><b>O nome do arquivo e sempre gerado aqui.</b> Usar o nome enviado pelo
 * navegador e o caminho classico para escrever fora da pasta de uploads — basta
 * um {@code ../../} no nome.
 *
 * <p><b>O tipo e conferido pelos bytes do arquivo</b>, nao pela extensao nem
 * pelo {@code Content-Type}: os dois sao informados por quem envia e mentem.
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final Path pasta;
    private final long tamanhoMaximoEmBytes;
    private final int larguraMaxima;
    private final Clock clock;

    public ImageService(GitgudProperties properties, Clock clock) {
        this.pasta = properties.uploads().dir().toAbsolutePath().normalize();
        this.tamanhoMaximoEmBytes = properties.uploads().maxSize().toBytes();
        this.larguraMaxima = properties.uploads().maxWidth();
        this.clock = clock;
    }

    public ImagemArmazenada armazenar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhum arquivo foi enviado.");
        }
        if (arquivo.getSize() > tamanhoMaximoEmBytes) {
            throw new BusinessException("Arquivo grande demais. O limite e "
                    + (tamanhoMaximoEmBytes / (1024 * 1024)) + " MB.");
        }

        byte[] bytes = ler(arquivo);
        FormatoDeImagem formato = FormatoDeImagem.reconhecer(bytes)
                .orElseThrow(() -> new BusinessException(
                        "O arquivo enviado nao e uma imagem JPEG, PNG, GIF ou WebP."));

        return formato.preservaAnimacao()
                ? guardarComoEsta(bytes, formato)
                : converterParaWebP(bytes);
    }

    /**
     * GIF passa direto.
     *
     * <p>Converter para WebP com o ImageIO manteria apenas o primeiro quadro, e
     * num blog de jogos o GIF animado e justamente o ponto.
     */
    private ImagemArmazenada guardarComoEsta(byte[] bytes, FormatoDeImagem formato) {
        BufferedImage imagem = decodificar(bytes);
        Path destino = destinoPara(formato.extensao());

        gravar(destino, saida -> saida.write(bytes));

        return new ImagemArmazenada(
                urlDe(destino), imagem.getWidth(), imagem.getHeight(), bytes.length);
    }

    private ImagemArmazenada converterParaWebP(byte[] bytes) {
        BufferedImage original = decodificar(bytes);
        BufferedImage final_ = limitarLargura(original);
        Path destino = destinoPara("webp");

        gravar(destino, saida -> {
            if (!ImageIO.write(final_, "webp", saida)) {
                throw new IOException("Nenhum escritor de WebP disponivel.");
            }
        });

        return new ImagemArmazenada(
                urlDe(destino), final_.getWidth(), final_.getHeight(), tamanhoDe(destino));
    }

    /**
     * Reduz imagem larga demais, mantendo a proporcao.
     *
     * <p>Nunca aumenta: esticar um print pequeno so deixaria o arquivo maior e a
     * imagem borrada.
     */
    private BufferedImage limitarLargura(BufferedImage original) {
        if (original.getWidth() <= larguraMaxima) {
            return original;
        }

        int altura = Math.max(1,
                Math.round(original.getHeight() * (larguraMaxima / (float) original.getWidth())));

        // Preserva o canal alfa quando existe; senao um PNG transparente virava
        // fundo preto.
        int tipo = original.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        BufferedImage reduzida = new BufferedImage(larguraMaxima, altura, tipo);
        var g = reduzida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, larguraMaxima, altura, null);
        g.dispose();

        return reduzida;
    }

    /** Caminho {@code {pasta}/AAAA/MM/{uuid}.{ext}}, com o nome sempre nosso. */
    private Path destinoPara(String extensao) {
        ZonedDateTime agora = ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        String nome = HexFormat.of().formatHex(uuidComoBytes()) + "." + extensao;

        return pasta
                .resolve("%04d".formatted(agora.getYear()))
                .resolve("%02d".formatted(agora.getMonthValue()))
                .resolve(nome);
    }

    private byte[] uuidComoBytes() {
        UUID uuid = UUID.randomUUID();
        return java.nio.ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    private String urlDe(Path destino) {
        return "/uploads/" + pasta.relativize(destino).toString().replace('\\', '/');
    }

    private BufferedImage decodificar(byte[] bytes) {
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(bytes));
            if (imagem == null) {
                throw new BusinessException("Nao foi possivel ler a imagem enviada.");
            }
            return imagem;
        } catch (IOException e) {
            throw new BusinessException("Nao foi possivel ler a imagem enviada.");
        }
    }

    private byte[] ler(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Nao foi possivel ler o arquivo enviado.");
        }
    }

    private void gravar(Path destino, Gravacao gravacao) {
        try {
            Files.createDirectories(destino.getParent());
            try (OutputStream saida = Files.newOutputStream(destino)) {
                gravacao.escrever(saida);
            }
            log.info("Imagem guardada em {}", destino);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar a imagem em " + destino, e);
        }
    }

    private long tamanhoDe(Path arquivo) {
        try {
            return Files.size(arquivo);
        } catch (IOException e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface Gravacao {
        void escrever(OutputStream saida) throws IOException;
    }
}
