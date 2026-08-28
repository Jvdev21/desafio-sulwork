package com.sulwork.desafio.controller;

import com.sulwork.desafio.dto.request.ItemCreateRequest;
import com.sulwork.desafio.dto.request.ItemStatusUpdateRequest;
import com.sulwork.desafio.dto.request.ItemUpdateRequest;
import com.sulwork.desafio.dto.response.ItemResponse;
import com.sulwork.desafio.service.ItemCafeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Tag(name = "Itens do café", description = "Opções informadas pelos participantes.")
public class ItemCafeController {

    private final ItemCafeService service;

    public ItemCafeController(ItemCafeService service) {
        this.service = service;
    }

    @PostMapping("/api/participacoes/{id}/itens")
    @Operation(summary = "Adicionar item a uma participação")
    @ApiResponse(responseCode = "201", description = "Item cadastrado.")
    @ApiResponse(responseCode = "404", description = "Participação não encontrada.")
    @ApiResponse(responseCode = "409", description = "Item duplicado no café.")
    public ResponseEntity<ItemResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        ItemResponse response = service.create(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/itens/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/api/itens/{id}")
    @Operation(summary = "Editar item")
    @ApiResponse(responseCode = "200", description = "Item atualizado.")
    @ApiResponse(responseCode = "404", description = "Item não encontrado.")
    @ApiResponse(responseCode = "409", description = "Item duplicado no café.")
    public ResponseEntity<ItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/api/itens/{id}/status")
    @Operation(summary = "Atualizar status do item")
    @ApiResponse(responseCode = "200", description = "Status atualizado.")
    @ApiResponse(responseCode = "400", description = "Transição ou data inválida.")
    @ApiResponse(responseCode = "404", description = "Item não encontrado.")
    public ResponseEntity<ItemResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ItemStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @DeleteMapping("/api/itens/{id}")
    @Operation(summary = "Excluir item")
    @ApiResponse(responseCode = "204", description = "Item excluído.")
    @ApiResponse(responseCode = "404", description = "Item não encontrado.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
