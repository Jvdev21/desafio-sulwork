package com.sulwork.desafio.repository;

import com.sulwork.desafio.domain.model.Colaborador;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColaboradorRepository extends Repository<Colaborador, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO colaborador (nome, cpf)
            VALUES (:nome, :cpf)
            """, nativeQuery = true)
    int insert(@Param("nome") String nome, @Param("cpf") String cpf);

    @Query(value = """
            SELECT id, nome, cpf, created_at, updated_at
            FROM colaborador
            ORDER BY id
            """, nativeQuery = true)
    List<Colaborador> findAllNative();

    @Query(value = """
            SELECT id, nome, cpf, created_at, updated_at
            FROM colaborador
            WHERE id = :id
            """, nativeQuery = true)
    Optional<Colaborador> findByIdNative(@Param("id") Long id);

    @Query(value = """
            SELECT id, nome, cpf, created_at, updated_at
            FROM colaborador
            WHERE cpf = :cpf
            """, nativeQuery = true)
    Optional<Colaborador> findByCpfNative(@Param("cpf") String cpf);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM colaborador
                WHERE cpf = :cpf
            )
            """, nativeQuery = true)
    boolean existsByCpfNative(@Param("cpf") String cpf);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM colaborador
                WHERE cpf = :cpf
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByCpfAndIdNotNative(@Param("cpf") String cpf, @Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE colaborador
            SET nome = :nome,
                cpf = :cpf,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    int update(
            @Param("id") Long id,
            @Param("nome") String nome,
            @Param("cpf") String cpf
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM colaborador
            WHERE id = :id
            """, nativeQuery = true)
    int delete(@Param("id") Long id);
}
