package com.sulwork.desafio.repository;

import com.sulwork.desafio.domain.model.Participacao;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipacaoRepository extends Repository<Participacao, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO participacao (colaborador_id, cafe_id)
            VALUES (:colaboradorId, :cafeId)
            """, nativeQuery = true)
    int insert(@Param("colaboradorId") Long colaboradorId, @Param("cafeId") Long cafeId);

    @Query(value = """
            SELECT id, colaborador_id, cafe_id, created_at
            FROM participacao
            WHERE id = :id
            """, nativeQuery = true)
    Optional<Participacao> findByIdNative(@Param("id") Long id);

    @Query(value = """
            SELECT id, colaborador_id, cafe_id, created_at
            FROM participacao
            WHERE colaborador_id = :colaboradorId AND cafe_id = :cafeId
            """, nativeQuery = true)
    Optional<Participacao> findByColaboradorAndCafeNative(
            @Param("colaboradorId") Long colaboradorId,
            @Param("cafeId") Long cafeId
    );

    @Query(value = """
            SELECT id, colaborador_id, cafe_id, created_at
            FROM participacao
            WHERE cafe_id = :cafeId
            ORDER BY id
            """, nativeQuery = true)
    List<Participacao> findAllByCafeNative(@Param("cafeId") Long cafeId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM participacao
                WHERE colaborador_id = :colaboradorId AND cafe_id = :cafeId
            )
            """, nativeQuery = true)
    boolean existsByColaboradorAndCafeNative(
            @Param("colaboradorId") Long colaboradorId,
            @Param("cafeId") Long cafeId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM participacao WHERE id = :id AND cafe_id = :cafeId", nativeQuery = true)
    int deleteFromCafe(@Param("id") Long id, @Param("cafeId") Long cafeId);
}
