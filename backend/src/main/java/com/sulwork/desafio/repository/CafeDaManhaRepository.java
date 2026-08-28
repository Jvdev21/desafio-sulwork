package com.sulwork.desafio.repository;

import com.sulwork.desafio.domain.model.CafeDaManha;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CafeDaManhaRepository extends Repository<CafeDaManha, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO cafe_da_manha (data) VALUES (:data)", nativeQuery = true)
    int insert(@Param("data") LocalDate data);

    @Query(value = "SELECT id, data, created_at FROM cafe_da_manha ORDER BY data, id", nativeQuery = true)
    List<CafeDaManha> findAllNative();

    @Query(value = "SELECT id, data, created_at FROM cafe_da_manha WHERE id = :id", nativeQuery = true)
    Optional<CafeDaManha> findByIdNative(@Param("id") Long id);

    @Query(value = "SELECT id, data, created_at FROM cafe_da_manha WHERE data = :data", nativeQuery = true)
    Optional<CafeDaManha> findByDataNative(@Param("data") LocalDate data);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM cafe_da_manha WHERE data = :data)", nativeQuery = true)
    boolean existsByDataNative(@Param("data") LocalDate data);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM cafe_da_manha
                WHERE data = :data AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByDataAndIdNotNative(@Param("data") LocalDate data, @Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE cafe_da_manha SET data = :data WHERE id = :id", nativeQuery = true)
    int update(@Param("id") Long id, @Param("data") LocalDate data);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM cafe_da_manha WHERE id = :id", nativeQuery = true)
    int delete(@Param("id") Long id);
}
