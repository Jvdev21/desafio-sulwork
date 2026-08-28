package com.sulwork.desafio.controller;

import com.sulwork.desafio.dto.request.CafeCreateRequest;
import com.sulwork.desafio.dto.request.CafeUpdateRequest;
import com.sulwork.desafio.dto.response.CafeResponse;
import com.sulwork.desafio.service.CafeDaManhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cafes")
@Tag(name = "Cafés da manhã", description = "Agenda de cafés da manhã.")
public class CafeDaManhaController {

    private final CafeDaManhaService service;

    public CafeDaManhaController(CafeDaManhaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar café da manhã")
    @ApiResponse(responseCode = "201", description = "Café cadastrado.")
    @ApiResponse(responseCode = "400", description = "Data inválida.")
    @ApiResponse(responseCode = "409", description = "Já existe café nesta data.")
    public ResponseEntity<CafeResponse> create(@Valid @RequestBody CafeCreateRequest request) {
        CafeResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar cafés da manhã")
    public ResponseEntity<List<CafeResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar café da manhã por ID")
    @ApiResponse(responseCode = "404", description = "Café não encontrado.")
    public ResponseEntity<CafeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar café da manhã")
    @ApiResponse(responseCode = "404", description = "Café não encontrado.")
    @ApiResponse(responseCode = "409", description = "Já existe café nesta data.")
    public ResponseEntity<CafeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CafeUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir café da manhã")
    @ApiResponse(responseCode = "204", description = "Café excluído.")
    @ApiResponse(responseCode = "404", description = "Café não encontrado.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
