package com.sulwork.desafio.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CafeResponse(
        Long id,
        LocalDate data,
        LocalDateTime createdAt
) {
}
