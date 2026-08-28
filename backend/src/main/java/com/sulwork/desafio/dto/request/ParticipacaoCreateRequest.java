package com.sulwork.desafio.dto.request;

import com.sulwork.desafio.validation.item.ValidItemName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ParticipacaoCreateRequest(
        @Schema(example = "1")
        @NotNull(message = "Colaborador é obrigatório.")
        Long colaboradorId,

        @Schema(example = "[\"Bolo\", \"Suco de Acerola\"]")
        @NotEmpty(message = "A participação deve possuir pelo menos um item.")
        List<@Valid @ValidItemName String> itens
) {
}
