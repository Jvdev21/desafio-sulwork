package com.sulwork.desafio.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CafeUpdateRequest(
        @Schema(example = "2030-09-01")
        @NotNull(message = "Data é obrigatória.")
        LocalDate data
) {
}
