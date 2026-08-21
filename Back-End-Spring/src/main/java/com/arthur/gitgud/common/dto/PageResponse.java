package com.arthur.gitgud.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginacao da API.
 *
 * <p>Existe para nao serializar o {@code Page} do Spring direto: o formato dele
 * e detalhe interno, muda entre versoes, e o proprio Spring avisa sobre isso.
 * Aqui o contrato e nosso e estavel.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> pagina, Function<E, T> conversor) {
        return new PageResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isLast());
    }
}
