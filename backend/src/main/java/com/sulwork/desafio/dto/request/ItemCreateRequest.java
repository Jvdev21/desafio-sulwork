package com.sulwork.desafio.dto.request;

import com.sulwork.desafio.validation.item.ValidItemName;
import io.swagger.v3.oas.annotations.media.Schema;

public record ItemCreateRequest(
        @Schema(example = "Pão de queijo")
        @ValidItemName
        String nome
) {
}
