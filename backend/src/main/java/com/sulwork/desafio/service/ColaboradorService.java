package com.sulwork.desafio.service;

import com.sulwork.desafio.domain.model.Colaborador;
import com.sulwork.desafio.dto.request.ColaboradorCreateRequest;
import com.sulwork.desafio.dto.request.ColaboradorUpdateRequest;
import com.sulwork.desafio.dto.response.ColaboradorResponse;
import com.sulwork.desafio.exception.BusinessRuleException;
import com.sulwork.desafio.exception.ConflictException;
import com.sulwork.desafio.exception.ResourceNotFoundException;
import com.sulwork.desafio.mapper.ColaboradorMapper;
import com.sulwork.desafio.repository.ColaboradorRepository;
import com.sulwork.desafio.validation.cpf.CpfNormalizer;
import com.sulwork.desafio.validation.cpf.CpfValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ColaboradorService {

    private static final String NOT_FOUND_MESSAGE = "Colaborador não encontrado.";
    private static final String DUPLICATE_CPF_MESSAGE = "CPF já cadastrado.";
    private static final String INVALID_CPF_MESSAGE = "CPF inválido.";

    private final ColaboradorRepository repository;

    public ColaboradorService(ColaboradorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ColaboradorResponse create(ColaboradorCreateRequest request) {
        String normalizedCpf = normalizeAndValidateCpf(request.cpf());
        if (repository.existsByCpfNative(normalizedCpf)) {
            throw new ConflictException(DUPLICATE_CPF_MESSAGE);
        }

        try {
            repository.insert(request.nome().trim(), normalizedCpf);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_CPF_MESSAGE);
        }

        return repository.findByCpfNative(normalizedCpf)
                .map(ColaboradorMapper::toResponse)
                .orElseThrow(() -> new IllegalStateException("Colaborador criado não foi localizado."));
    }

    @Transactional(readOnly = true)
    public List<ColaboradorResponse> findAll() {
        return repository.findAllNative().stream()
                .map(ColaboradorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColaboradorResponse findById(Long id) {
        return ColaboradorMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ColaboradorResponse update(Long id, ColaboradorUpdateRequest request) {
        findEntityById(id);

        String normalizedCpf = normalizeAndValidateCpf(request.cpf());
        if (repository.existsByCpfAndIdNotNative(normalizedCpf, id)) {
            throw new ConflictException(DUPLICATE_CPF_MESSAGE);
        }

        try {
            int updatedRows = repository.update(id, request.nome().trim(), normalizedCpf);
            if (updatedRows == 0) {
                throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_CPF_MESSAGE);
        }

        return repository.findByIdNative(id)
                .map(ColaboradorMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
    }

    @Transactional
    public void delete(Long id) {
        if (repository.delete(id) == 0) {
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        }
    }

    private Colaborador findEntityById(Long id) {
        return repository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
    }

    private String normalizeAndValidateCpf(String cpf) {
        final String normalizedCpf;
        try {
            normalizedCpf = CpfNormalizer.normalize(cpf);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException(INVALID_CPF_MESSAGE);
        }

        if (!CpfValidator.isValid(normalizedCpf)) {
            throw new BusinessRuleException(INVALID_CPF_MESSAGE);
        }
        return normalizedCpf;
    }
}
