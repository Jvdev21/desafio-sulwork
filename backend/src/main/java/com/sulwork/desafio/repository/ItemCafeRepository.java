package com.sulwork.desafio.repository;

import com.sulwork.desafio.domain.model.ItemCafe;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemCafeRepository extends Repository<ItemCafe, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO item_cafe (participacao_id, cafe_id, nome, nome_normalizado, status)
            VALUES (:participacaoId, :cafeId, :nome, :nomeNormalizado, 'PENDENTE')
            """, nativeQuery = true)
    int insert(
            @Param("participacaoId") Long participacaoId,
            @Param("cafeId") Long cafeId,
            @Param("nome") String nome,
            @Param("nomeNormalizado") String nomeNormalizado
    );

    @Query(value = """
            SELECT id, participacao_id, cafe_id, nome, nome_normalizado, status, created_at, updated_at
            FROM item_cafe WHERE id = :id
            """, nativeQuery = true)
    Optional<ItemCafe> findByIdNative(@Param("id") Long id);

    @Query(value = """
            SELECT id, participacao_id, cafe_id, nome, nome_normalizado, status, created_at, updated_at
            FROM item_cafe WHERE participacao_id = :participacaoId ORDER BY id
            """, nativeQuery = true)
    List<ItemCafe> findAllByParticipacaoNative(@Param("participacaoId") Long participacaoId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM item_cafe
                WHERE cafe_id = :cafeId AND nome_normalizado = :nomeNormalizado
            )
            """, nativeQuery = true)
    boolean existsByCafeAndNomeNormalizadoNative(
            @Param("cafeId") Long cafeId,
            @Param("nomeNormalizado") String nomeNormalizado
    );

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM item_cafe
                WHERE cafe_id = :cafeId
                  AND nome_normalizado = :nomeNormalizado
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByCafeAndNomeNormalizadoAndIdNotNative(
            @Param("cafeId") Long cafeId,
            @Param("nomeNormalizado") String nomeNormalizado,
            @Param("id") Long id
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE item_cafe
            SET nome = :nome,
                nome_normalizado = :nomeNormalizado,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    int update(
            @Param("id") Long id,
            @Param("nome") String nome,
            @Param("nomeNormalizado") String nomeNormalizado
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE item_cafe
            SET status = :status,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM item_cafe WHERE id = :id", nativeQuery = true)
    int delete(@Param("id") Long id);
}
