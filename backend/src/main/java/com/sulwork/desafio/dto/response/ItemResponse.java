package com.sulwork.desafio.dto.response;

import com.sulwork.desafio.domain.model.ItemCafeStatus;

import java.time.LocalDateTime;

public record ItemResponse(
        Long id,
        Long participacaoId,
        Long cafeId,
        String nome,
        ItemCafeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
