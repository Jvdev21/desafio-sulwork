package com.sulwork.desafio.controller;

import com.sulwork.desafio.dto.request.ParticipacaoCreateRequest;
import com.sulwork.desafio.dto.response.ParticipacaoResponse;
import com.sulwork.desafio.service.ParticipacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cafes/{cafeId}/participantes")
@Tag(name = "Participações", description = "Participantes e suas opções por café.")
public class ParticipacaoController {

    private final ParticipacaoService service;

    public ParticipacaoController(ParticipacaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Adicionar participante ao café")
    @ApiResponse(responseCode = "201", description = "Participação cadastrada.")
    @ApiResponse(responseCode = "404", description = "Café ou colaborador não encontrado.")
    @ApiResponse(responseCode = "409", description = "Participação ou item duplicado.")
    public ResponseEntity<ParticipacaoResponse> create(
            @PathVariable Long cafeId,
            @Valid @RequestBody ParticipacaoCreateRequest request
    ) {
        ParticipacaoResponse response = service.create(cafeId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/cafes/{cafeId}/participantes/{id}")
                .buildAndExpand(cafeId, response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar participantes do café")
    @ApiResponse(responseCode = "404", description = "Café não encontrado.")
    public ResponseEntity<List<ParticipacaoResponse>> findAll(@PathVariable Long cafeId) {
        return ResponseEntity.ok(service.findAllByCafe(cafeId));
    }

    @DeleteMapping("/{participacaoId}")
    @Operation(summary = "Remover participante do café")
    @ApiResponse(responseCode = "204", description = "Participação removida.")
    @ApiResponse(responseCode = "404", description = "Café ou participação não encontrada.")
    public ResponseEntity<Void> delete(
            @PathVariable Long cafeId,
            @PathVariable Long participacaoId
    ) {
        service.delete(cafeId, participacaoId);
        return ResponseEntity.noContent().build();
    }
}
