package com.sulwork.desafio.service;

import com.sulwork.desafio.config.CurrentDateProvider;
import com.sulwork.desafio.domain.model.CafeDaManha;
import com.sulwork.desafio.domain.model.ItemCafe;
import com.sulwork.desafio.domain.model.ItemCafeStatus;
import com.sulwork.desafio.domain.model.Participacao;
import com.sulwork.desafio.dto.request.ItemCreateRequest;
import com.sulwork.desafio.dto.request.ItemStatusUpdateRequest;
import com.sulwork.desafio.dto.request.ItemUpdateRequest;
import com.sulwork.desafio.dto.response.ItemResponse;
import com.sulwork.desafio.exception.BusinessRuleException;
import com.sulwork.desafio.exception.ConflictException;
import com.sulwork.desafio.exception.ResourceNotFoundException;
import com.sulwork.desafio.repository.CafeDaManhaRepository;
import com.sulwork.desafio.repository.ItemCafeRepository;
import com.sulwork.desafio.repository.ParticipacaoRepository;
import com.sulwork.desafio.validation.item.ItemNameNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ItemCafeService {

    private static final String ITEM_NOT_FOUND_MESSAGE = "Item não encontrado.";
    private static final String PARTICIPATION_NOT_FOUND_MESSAGE = "Participação não encontrada.";
    private static final String INVALID_NAME_MESSAGE =
            "Nome do item deve ser informado e possuir no máximo 120 caracteres.";
    private static final String ITEM_MUTATION_DATE_MESSAGE =
            "Itens só podem ser adicionados, editados ou removidos antes da data do café.";
    private static final String STATUS_BEFORE_DATE_MESSAGE =
            "O status do item só pode ser alterado na data do café.";
    private static final String STATUS_AFTER_DATE_MESSAGE =
            "O status do item não pode ser alterado após a data do café.";
    private static final String PENDING_RETURN_MESSAGE =
            "Não é permitido alterar o status para PENDENTE no dia do café.";
    private final ItemCafeRepository itemRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final CafeDaManhaRepository cafeRepository;
    private final CurrentDateProvider currentDateProvider;

    public ItemCafeService(
            ItemCafeRepository itemRepository,
            ParticipacaoRepository participacaoRepository,
            CafeDaManhaRepository cafeRepository,
            CurrentDateProvider currentDateProvider
    ) {
        this.itemRepository = itemRepository;
        this.participacaoRepository = participacaoRepository;
        this.cafeRepository = cafeRepository;
        this.currentDateProvider = currentDateProvider;
    }

    @Transactional
    public ItemResponse create(Long participacaoId, ItemCreateRequest request) {
        Participacao participacao = requireParticipation(participacaoId);
        ensureCafeIsFuture(participacao.getCafeId());
        return createForParticipation(participacao, request.nome());
    }

    @Transactional
    public ItemResponse createForParticipation(Participacao participacao, String rawName) {
        ensureCafeIsFuture(participacao.getCafeId());
        PreparedName name = prepare(rawName);
        ensureAvailable(participacao.getCafeId(), name.normalized(), name.display(), null);
        try {
            itemRepository.insert(
                    participacao.getId(),
                    participacao.getCafeId(),
                    name.display(),
                    name.normalized()
            );
        } catch (DataIntegrityViolationException exception) {
            throw duplicateItem(name.display());
        }
        return itemRepository.findAllByParticipacaoNative(participacao.getId()).stream()
                .filter(item -> item.getNomeNormalizado().equals(name.normalized()))
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Item criado não foi localizado."));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        ItemCafe item = requireItem(id);
        requireParticipation(item.getParticipacaoId());
        ensureCafeIsFuture(item.getCafeId());
        PreparedName name = prepare(request.nome());
        ensureAvailable(item.getCafeId(), name.normalized(), name.display(), id);
        try {
            if (itemRepository.update(id, name.display(), name.normalized()) == 0) {
                throw new ResourceNotFoundException(ITEM_NOT_FOUND_MESSAGE);
            }
        } catch (DataIntegrityViolationException exception) {
            throw duplicateItem(name.display());
        }
        return toResponse(requireItem(id));
    }

    @Transactional
    public void delete(Long id) {
        ItemCafe item = requireItem(id);
        requireParticipation(item.getParticipacaoId());
        ensureCafeIsFuture(item.getCafeId());
        if (itemRepository.delete(id) == 0) {
            throw new ResourceNotFoundException(ITEM_NOT_FOUND_MESSAGE);
        }
    }

    @Transactional
    public ItemResponse updateStatus(Long id, ItemStatusUpdateRequest request) {
        ItemCafe item = requireItem(id);
        LocalDate cafeDate = requireCafe(item.getCafeId()).getData();
        LocalDate today = currentDateProvider.today();

        if (cafeDate.isAfter(today)) {
            throw new BusinessRuleException(STATUS_BEFORE_DATE_MESSAGE);
        }
        if (cafeDate.isBefore(today)) {
            throw new BusinessRuleException(STATUS_AFTER_DATE_MESSAGE);
        }
        if (request.status() == ItemCafeStatus.PENDENTE) {
            throw new BusinessRuleException(PENDING_RETURN_MESSAGE);
        }

        if (itemRepository.updateStatus(id, request.status().name()) == 0) {
            throw new ResourceNotFoundException(ITEM_NOT_FOUND_MESSAGE);
        }
        return toResponse(requireItem(id));
    }

    public PreparedName prepare(String rawName) {
        if (rawName == null || rawName.isBlank() || rawName.length() > 120) {
            throw new BusinessRuleException(INVALID_NAME_MESSAGE);
        }
        return new PreparedName(rawName.trim(), ItemNameNormalizer.normalize(rawName));
    }

    public void ensureAvailable(Long cafeId, String normalizedName, String displayName, Long ignoredItemId) {
        boolean duplicate = ignoredItemId == null
                ? itemRepository.existsByCafeAndNomeNormalizadoNative(cafeId, normalizedName)
                : itemRepository.existsByCafeAndNomeNormalizadoAndIdNotNative(
                        cafeId, normalizedName, ignoredItemId
                );
        if (duplicate) {
            throw duplicateItem(displayName);
        }
    }

    public ItemResponse toResponse(ItemCafe item) {
        ItemCafeStatus effectiveStatus = effectiveStatus(item);
        return new ItemResponse(
                item.getId(),
                item.getParticipacaoId(),
                item.getCafeId(),
                item.getNome(),
                effectiveStatus,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private ItemCafeStatus effectiveStatus(ItemCafe item) {
        LocalDate cafeDate = requireCafe(item.getCafeId()).getData();
        if (cafeDate.isBefore(currentDateProvider.today())
                && item.getStatus() == ItemCafeStatus.PENDENTE) {
            return ItemCafeStatus.NAO_TROUXE;
        }
        return item.getStatus();
    }

    private void ensureCafeIsFuture(Long cafeId) {
        if (!requireCafe(cafeId).getData().isAfter(currentDateProvider.today())) {
            throw new BusinessRuleException(ITEM_MUTATION_DATE_MESSAGE);
        }
    }

    private CafeDaManha requireCafe(Long id) {
        return cafeRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException("Café da manhã não encontrado."));
    }

    private Participacao requireParticipation(Long id) {
        return participacaoRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException(PARTICIPATION_NOT_FOUND_MESSAGE));
    }

    private ItemCafe requireItem(Long id) {
        return itemRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException(ITEM_NOT_FOUND_MESSAGE));
    }

    private ConflictException duplicateItem(String displayName) {
        return new ConflictException("A opção '" + displayName + "' já foi cadastrada para este café.");
    }

    public record PreparedName(String display, String normalized) {
    }
}
