package com.sulwork.desafio.controller;

import com.sulwork.desafio.dto.request.ColaboradorCreateRequest;
import com.sulwork.desafio.dto.request.ColaboradorUpdateRequest;
import com.sulwork.desafio.dto.response.ColaboradorResponse;
import com.sulwork.desafio.service.ColaboradorService;
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
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores", description = "Cadastro e manutenção de colaboradores.")
public class ColaboradorController {

    private final ColaboradorService service;

    public ColaboradorController(ColaboradorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar colaborador")
    @ApiResponse(responseCode = "201", description = "Colaborador cadastrado.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "409", description = "CPF já cadastrado.")
    public ResponseEntity<ColaboradorResponse> create(
            @Valid @RequestBody ColaboradorCreateRequest request
    ) {
        ColaboradorResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar colaboradores")
    @ApiResponse(responseCode = "200", description = "Colaboradores listados.")
    public ResponseEntity<List<ColaboradorResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar colaborador por ID")
    @ApiResponse(responseCode = "200", description = "Colaborador encontrado.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    public ResponseEntity<ColaboradorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar colaborador")
    @ApiResponse(responseCode = "200", description = "Colaborador atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    @ApiResponse(responseCode = "409", description = "CPF já cadastrado.")
    public ResponseEntity<ColaboradorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ColaboradorUpdateRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir colaborador")
    @ApiResponse(responseCode = "204", description = "Colaborador excluído.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
