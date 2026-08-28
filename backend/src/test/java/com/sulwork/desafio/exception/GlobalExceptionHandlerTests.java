package com.sulwork.desafio.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-08-26T20:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        handler = new GlobalExceptionHandler(fixedClock);
        request = new MockHttpServletRequest("GET", "/api/colaboradores/99");
    }

    @Test
    void mapsDomainExceptionsToCoherentStatuses() {
        assertThat(handler.handleResourceNotFound(
                new ResourceNotFoundException("Colaborador não encontrado."), request
        ).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(handler.handleConflict(
                new ConflictException("CPF já cadastrado."), request
        ).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(handler.handleBusinessRule(
                new BusinessRuleException("Regra inválida."), request
        ).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unexpectedErrorResponseDoesNotExposeInternalMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("senha=segredo; SQL SELECT * FROM colaborador"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Ocorreu um erro interno inesperado.");
        assertThat(response.getBody().message()).doesNotContain("senha", "SQL");
        assertThat(response.getBody().path()).isEqualTo("/api/colaboradores/99");
        assertThat(response.getBody().timestamp()).isEqualTo(LocalDateTime.of(2026, 8, 26, 17, 0));
        assertThat(response.getBody().fields()).isEmpty();
    }
}
