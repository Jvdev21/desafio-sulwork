package com.sulwork.desafio.dto.request;

import com.sulwork.desafio.validation.item.ValidItemName;
import io.swagger.v3.oas.annotations.media.Schema;

public record ItemUpdateRequest(
        @Schema(example = "Suco de laranja")
        @ValidItemName
        String nome
) {
}
