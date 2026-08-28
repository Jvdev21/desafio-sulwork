package com.sulwork.desafio.service;

import com.sulwork.desafio.config.CurrentDateProvider;
import com.sulwork.desafio.domain.model.CafeDaManha;
import com.sulwork.desafio.dto.request.CafeCreateRequest;
import com.sulwork.desafio.dto.request.CafeUpdateRequest;
import com.sulwork.desafio.dto.response.CafeResponse;
import com.sulwork.desafio.exception.BusinessRuleException;
import com.sulwork.desafio.exception.ConflictException;
import com.sulwork.desafio.exception.ResourceNotFoundException;
import com.sulwork.desafio.repository.CafeDaManhaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CafeDaManhaService {

    public static final String NOT_FOUND_MESSAGE = "Café da manhã não encontrado.";
    private static final String INVALID_DATE_MESSAGE = "A data do café deve ser maior que a data atual.";
    private static final String DUPLICATE_DATE_MESSAGE = "Já existe um café cadastrado para esta data.";
    private static final String IMMUTABLE_CAFE_MESSAGE =
            "Cafés do dia ou passados não podem ser alterados ou excluídos.";
    private final CafeDaManhaRepository repository;
    private final CurrentDateProvider currentDateProvider;

    public CafeDaManhaService(CafeDaManhaRepository repository, CurrentDateProvider currentDateProvider) {
        this.repository = repository;
        this.currentDateProvider = currentDateProvider;
    }

    @Transactional
    public CafeResponse create(CafeCreateRequest request) {
        validateFutureDate(request.data());
        if (repository.existsByDataNative(request.data())) {
            throw new ConflictException(DUPLICATE_DATE_MESSAGE);
        }
        try {
            repository.insert(request.data());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_DATE_MESSAGE);
        }
        return repository.findByDataNative(request.data())
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Café criado não foi localizado."));
    }

    @Transactional(readOnly = true)
    public List<CafeResponse> findAll() {
        return repository.findAllNative().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CafeResponse findById(Long id) {
        return toResponse(requireEntity(id));
    }

    @Transactional
    public CafeResponse update(Long id, CafeUpdateRequest request) {
        CafeDaManha currentCafe = requireEntity(id);
        ensureCafeIsFuture(currentCafe);
        validateFutureDate(request.data());
        if (repository.existsByDataAndIdNotNative(request.data(), id)) {
            throw new ConflictException(DUPLICATE_DATE_MESSAGE);
        }
        try {
            if (repository.update(id, request.data()) == 0) {
                throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_DATE_MESSAGE);
        }
        return toResponse(requireEntity(id));
    }

    @Transactional
    public void delete(Long id) {
        ensureCafeIsFuture(requireEntity(id));
        if (repository.delete(id) == 0) {
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public CafeDaManha requireEntity(Long id) {
        return repository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
    }

    private void validateFutureDate(LocalDate data) {
        if (!data.isAfter(currentDateProvider.today())) {
            throw new BusinessRuleException(INVALID_DATE_MESSAGE);
        }
    }

    private void ensureCafeIsFuture(CafeDaManha cafe) {
        if (!cafe.getData().isAfter(currentDateProvider.today())) {
            throw new BusinessRuleException(IMMUTABLE_CAFE_MESSAGE);
        }
    }

    private CafeResponse toResponse(CafeDaManha cafe) {
        return new CafeResponse(
                cafe.getId(),
                cafe.getData(),
                cafe.getCreatedAt()
        );
    }
}
