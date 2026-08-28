package com.sulwork.desafio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ParticipacaoResponse(
        Long id,
        Long cafeId,
        Long colaboradorId,
        LocalDateTime createdAt,
        List<ItemResponse> itens
) {
}
