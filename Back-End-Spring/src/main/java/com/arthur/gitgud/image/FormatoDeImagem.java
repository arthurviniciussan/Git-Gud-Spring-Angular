package com.arthur.gitgud.image;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos aceitos, reconhecidos pelos primeiros bytes do arquivo.
 *
 * <p>Extensao e {@code Content-Type} vem de quem envia e podem mentir; os bytes
 * de assinatura, nao. E o que impede subir um script com nome {@code .png}.
 */
public enum FormatoDeImagem {

    JPEG("jpg", false, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", false, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    GIF("gif", true, new byte[] {'G', 'I', 'F', '8'}),
    WEBP("webp", false, new byte[] {'R', 'I', 'F', 'F'});

    private final String extensao;
    private final boolean preservaAnimacao;
    private final byte[] assinatura;

    FormatoDeImagem(String extensao, boolean preservaAnimacao, byte[] assinatura) {
        this.extensao = extensao;
        this.preservaAnimacao = preservaAnimacao;
        this.assinatura = assinatura;
    }

    public static Optional<FormatoDeImagem> reconhecer(byte[] bytes) {
        return Arrays.stream(values())
                .filter(formato -> formato.combina(bytes))
                .findFirst();
    }

    private boolean combina(byte[] bytes) {
        if (bytes.length < assinatura.length) {
            return false;
        }
        if (!Arrays.equals(Arrays.copyOfRange(bytes, 0, assinatura.length), assinatura)) {
            return false;
        }
        // "RIFF" sozinho tambem e WAV e AVI: o WebP so se confirma pelos bytes 8-11.
        return this != WEBP
                || (bytes.length >= 12
                    && Arrays.equals(Arrays.copyOfRange(bytes, 8, 12), new byte[] {'W', 'E', 'B', 'P'}));
    }

    public String extensao() {
        return extensao;
    }

    public boolean preservaAnimacao() {
        return preservaAnimacao;
    }
}
