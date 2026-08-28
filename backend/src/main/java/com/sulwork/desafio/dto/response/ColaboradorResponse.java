package com.sulwork.desafio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ColaboradorResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "João da Silva")
        String nome,

        @Schema(example = "73244216013", description = "CPF normalizado, sem máscara.")
        String cpf,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
