package com.sulwork.desafio.dto.request;

import com.sulwork.desafio.validation.cpf.ValidCpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColaboradorUpdateRequest(
        @Schema(example = "João da Silva")
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve possuir no máximo 150 caracteres.")
        String nome,

        @Schema(example = "73244216013", description = "CPF com ou sem máscara; armazenado normalizado.")
        @NotBlank(message = "CPF é obrigatório.")
        @ValidCpf
        String cpf
) {
}
