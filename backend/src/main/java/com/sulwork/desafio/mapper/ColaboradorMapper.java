package com.sulwork.desafio.mapper;

import com.sulwork.desafio.domain.model.Colaborador;
import com.sulwork.desafio.dto.response.ColaboradorResponse;

public final class ColaboradorMapper {

    private ColaboradorMapper() {
    }

    public static ColaboradorResponse toResponse(Colaborador colaborador) {
        return new ColaboradorResponse(
                colaborador.getId(),
                colaborador.getNome(),
                colaborador.getCpf(),
                colaborador.getCreatedAt(),
                colaborador.getUpdatedAt()
        );
    }
}
