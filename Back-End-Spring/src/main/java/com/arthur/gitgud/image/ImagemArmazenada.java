package com.arthur.gitgud.image;

/** Resultado de um upload: onde a imagem ficou e em que tamanho. */
public record ImagemArmazenada(String url, int largura, int altura, long bytes) {
}
