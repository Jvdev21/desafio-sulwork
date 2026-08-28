package com.sulwork.desafio.service;

import com.sulwork.desafio.domain.model.Colaborador;
import com.sulwork.desafio.dto.request.ColaboradorCreateRequest;
import com.sulwork.desafio.dto.request.ColaboradorUpdateRequest;
import com.sulwork.desafio.dto.response.ColaboradorResponse;
import com.sulwork.desafio.exception.BusinessRuleException;
import com.sulwork.desafio.exception.ConflictException;
import com.sulwork.desafio.exception.ResourceNotFoundException;
import com.sulwork.desafio.repository.ColaboradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColaboradorServiceTests {

    @Mock
    private ColaboradorRepository repository;

    private ColaboradorService service;

    @BeforeEach
    void setUp() {
        service = new ColaboradorService(repository);
    }

    @Test
    void createsCollaboratorWithNormalizedCpfAndTrimmedName() {
        Colaborador entity = collaborator(1L, "Maria Silva", "52998224725");
        when(repository.existsByCpfNative("52998224725")).thenReturn(false);
        when(repository.insert("Maria Silva", "52998224725")).thenReturn(1);
        when(repository.findByCpfNative("52998224725")).thenReturn(Optional.of(entity));

        ColaboradorResponse response = service.create(
                new ColaboradorCreateRequest("  Maria Silva  ", "529.982.247-25")
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.cpf()).isEqualTo("52998224725");
        verify(repository).insert("Maria Silva", "52998224725");
    }

    @Test
    void rejectsDuplicateCpfBeforeInsert() {
        when(repository.existsByCpfNative("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new ColaboradorCreateRequest("Maria", "52998224725")
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("CPF já cadastrado.");

        verify(repository, never()).insert("Maria", "52998224725");
    }

    @Test
    void rejectsInvalidCpfDefensively() {
        assertThatThrownBy(() -> service.create(
                new ColaboradorCreateRequest("Maria", "12345678901")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("CPF inválido.");
    }

    @Test
    void listsAndFindsCollaborators() {
        Colaborador maria = collaborator(1L, "Maria", "52998224725");
        Colaborador joao = collaborator(2L, "João", "11144477735");
        when(repository.findAllNative()).thenReturn(List.of(maria, joao));
        when(repository.findByIdNative(2L)).thenReturn(Optional.of(joao));

        assertThat(service.findAll()).extracting(ColaboradorResponse::id).containsExactly(1L, 2L);
        assertThat(service.findById(2L).nome()).isEqualTo("João");
    }

    @Test
    void reportsMissingCollaborator() {
        when(repository.findByIdNative(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Colaborador não encontrado.");
    }

    @Test
    void updatesCollaboratorWhileKeepingOwnCpf() {
        Colaborador existing = collaborator(1L, "Maria", "52998224725");
        Colaborador updated = collaborator(1L, "Maria Souza", "52998224725");
        when(repository.findByIdNative(1L))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(updated));
        when(repository.existsByCpfAndIdNotNative("52998224725", 1L)).thenReturn(false);
        when(repository.update(1L, "Maria Souza", "52998224725")).thenReturn(1);

        ColaboradorResponse response = service.update(
                1L,
                new ColaboradorUpdateRequest(" Maria Souza ", "529.982.247-25")
        );

        assertThat(response.nome()).isEqualTo("Maria Souza");
        verify(repository).update(1L, "Maria Souza", "52998224725");
    }

    @Test
    void preventsUsingCpfFromAnotherCollaborator() {
        Colaborador existing = collaborator(1L, "Maria", "52998224725");
        when(repository.findByIdNative(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCpfAndIdNotNative("11144477735", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                1L,
                new ColaboradorUpdateRequest("Maria", "11144477735")
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("CPF já cadastrado.");

        verify(repository, never()).update(1L, "Maria", "11144477735");
    }

    @Test
    void deletesExistingCollaboratorAndReportsMissingId() {
        when(repository.delete(1L)).thenReturn(1);
        when(repository.delete(99L)).thenReturn(0);

        service.delete(1L);
        verify(repository).delete(1L);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Colaborador não encontrado.");
    }

    private Colaborador collaborator(Long id, String nome, String cpf) {
        Colaborador colaborador = mock(Colaborador.class);
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 26, 12, 0);
        lenient().when(colaborador.getId()).thenReturn(id);
        lenient().when(colaborador.getNome()).thenReturn(nome);
        lenient().when(colaborador.getCpf()).thenReturn(cpf);
        lenient().when(colaborador.getCreatedAt()).thenReturn(timestamp);
        lenient().when(colaborador.getUpdatedAt()).thenReturn(timestamp);
        return colaborador;
    }
}
