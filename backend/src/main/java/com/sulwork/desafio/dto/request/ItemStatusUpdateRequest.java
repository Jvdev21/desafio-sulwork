package com.sulwork.desafio.dto.request;

import com.sulwork.desafio.domain.model.ItemCafeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ItemStatusUpdateRequest(
        @Schema(example = "TROUXE", allowableValues = {"TROUXE", "NAO_TROUXE"})
        @NotNull(message = "Status é obrigatório.")
        ItemCafeStatus status
) {
}
