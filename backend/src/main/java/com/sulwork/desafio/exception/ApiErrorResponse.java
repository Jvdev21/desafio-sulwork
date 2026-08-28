package com.sulwork.desafio.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> fields
) {

    public ApiErrorResponse {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
