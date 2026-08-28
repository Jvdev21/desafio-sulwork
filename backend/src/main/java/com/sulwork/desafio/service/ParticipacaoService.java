package com.sulwork.desafio.service;

import com.sulwork.desafio.config.CurrentDateProvider;
import com.sulwork.desafio.domain.model.CafeDaManha;
import com.sulwork.desafio.domain.model.Participacao;
import com.sulwork.desafio.dto.request.ParticipacaoCreateRequest;
import com.sulwork.desafio.dto.response.ParticipacaoResponse;
import com.sulwork.desafio.exception.BusinessRuleException;
import com.sulwork.desafio.exception.ConflictException;
import com.sulwork.desafio.exception.ResourceNotFoundException;
import com.sulwork.desafio.repository.CafeDaManhaRepository;
import com.sulwork.desafio.repository.ColaboradorRepository;
import com.sulwork.desafio.repository.ItemCafeRepository;
import com.sulwork.desafio.repository.ParticipacaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParticipacaoService {

    private static final String CAFE_NOT_FOUND_MESSAGE = "Café da manhã não encontrado.";
    private static final String COLLABORATOR_NOT_FOUND_MESSAGE = "Colaborador não encontrado.";
    private static final String PARTICIPATION_NOT_FOUND_MESSAGE = "Participação não encontrada.";
    private static final String DUPLICATE_PARTICIPATION_MESSAGE =
            "Colaborador já participa deste café.";
    private static final String PARTICIPATION_MUTATION_DATE_MESSAGE =
            "Participações só podem ser adicionadas ou removidas antes da data do café.";
    private final ParticipacaoRepository repository;
    private final CafeDaManhaRepository cafeRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ItemCafeRepository itemRepository;
    private final ItemCafeService itemService;
    private final CurrentDateProvider currentDateProvider;

    public ParticipacaoService(
            ParticipacaoRepository repository,
            CafeDaManhaRepository cafeRepository,
            ColaboradorRepository colaboradorRepository,
            ItemCafeRepository itemRepository,
            ItemCafeService itemService,
            CurrentDateProvider currentDateProvider
    ) {
        this.repository = repository;
        this.cafeRepository = cafeRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.currentDateProvider = currentDateProvider;
    }

    @Transactional
    public ParticipacaoResponse create(Long cafeId, ParticipacaoCreateRequest request) {
        CafeDaManha cafe = requireCafe(cafeId);
        ensureCafeIsFuture(cafe);
        if (colaboradorRepository.findByIdNative(request.colaboradorId()).isEmpty()) {
            throw new ResourceNotFoundException(COLLABORATOR_NOT_FOUND_MESSAGE);
        }
        if (repository.existsByColaboradorAndCafeNative(request.colaboradorId(), cafeId)) {
            throw new ConflictException(DUPLICATE_PARTICIPATION_MESSAGE);
        }

        Map<String, ItemCafeService.PreparedName> names = new LinkedHashMap<>();
        for (String rawName : request.itens()) {
            ItemCafeService.PreparedName name = itemService.prepare(rawName);
            if (names.putIfAbsent(name.normalized(), name) != null) {
                throw duplicateItem(name.display());
            }
            itemService.ensureAvailable(cafeId, name.normalized(), name.display(), null);
        }

        try {
            repository.insert(request.colaboradorId(), cafeId);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(DUPLICATE_PARTICIPATION_MESSAGE);
        }

        Participacao participation = repository.findByColaboradorAndCafeNative(
                request.colaboradorId(), cafeId
        ).orElseThrow(() -> new IllegalStateException("Participação criada não foi localizada."));

        for (ItemCafeService.PreparedName name : names.values()) {
            itemService.createForParticipation(participation, name.display());
        }
        return toResponse(participation);
    }

    @Transactional(readOnly = true)
    public List<ParticipacaoResponse> findAllByCafe(Long cafeId) {
        requireCafe(cafeId);
        return repository.findAllByCafeNative(cafeId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long cafeId, Long participationId) {
        ensureCafeIsFuture(requireCafe(cafeId));
        if (repository.deleteFromCafe(participationId, cafeId) == 0) {
            throw new ResourceNotFoundException(PARTICIPATION_NOT_FOUND_MESSAGE);
        }
    }

    private CafeDaManha requireCafe(Long cafeId) {
        return cafeRepository.findByIdNative(cafeId)
                .orElseThrow(() -> new ResourceNotFoundException(CAFE_NOT_FOUND_MESSAGE));
    }

    private void ensureCafeIsFuture(CafeDaManha cafe) {
        if (!cafe.getData().isAfter(currentDateProvider.today())) {
            throw new BusinessRuleException(PARTICIPATION_MUTATION_DATE_MESSAGE);
        }
    }

    private ParticipacaoResponse toResponse(Participacao participation) {
        return new ParticipacaoResponse(
                participation.getId(),
                participation.getCafeId(),
                participation.getColaboradorId(),
                participation.getCreatedAt(),
                itemRepository.findAllByParticipacaoNative(participation.getId()).stream()
                        .map(itemService::toResponse)
                        .toList()
        );
    }

    private ConflictException duplicateItem(String displayName) {
        return new ConflictException("A opção '" + displayName + "' já foi cadastrada para este café.");
    }
}
