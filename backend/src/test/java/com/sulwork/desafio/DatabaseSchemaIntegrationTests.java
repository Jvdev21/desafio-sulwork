package com.sulwork.desafio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseSchemaIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("desafio_sulwork_test")
            .withUsername("desafio_sulwork")
            .withPassword("desafio_sulwork_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearDomainTables() {
        jdbc.execute("TRUNCATE TABLE item_cafe, participacao, cafe_da_manha, colaborador RESTART IDENTITY CASCADE");
    }

    @Test
    void flywayCreatesTheExpectedSchemaAndNamedConstraints() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);

        assertThat(tables).contains(
                "flyway_schema_history",
                "colaborador",
                "cafe_da_manha",
                "participacao",
                "item_cafe"
        );

        Integer appliedMigrations = jdbc.queryForObject("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version IN ('1', '2', '3', '4') AND success
                """, Integer.class);
        assertThat(appliedMigrations).isEqualTo(4);

        List<String> constraints = jdbc.queryForList("""
                SELECT conname
                FROM pg_constraint
                WHERE connamespace = 'public'::regnamespace
                """, String.class);
        assertThat(constraints).contains(
                "pk_colaborador",
                "uk_colaborador_cpf",
                "pk_cafe_da_manha",
                "uk_cafe_da_manha_data",
                "pk_participacao",
                "fk_participacao_colaborador",
                "fk_participacao_cafe",
                "uk_participacao_colaborador_cafe",
                "uk_participacao_id_cafe",
                "pk_item_cafe",
                "fk_item_cafe_participacao",
                "fk_item_cafe_cafe",
                "uk_item_cafe_cafe_nome_normalizado",
                "ck_item_cafe_status"
        );

        List<String> indexes = jdbc.queryForList("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                """, String.class);
        assertThat(indexes).contains(
                "idx_participacao_cafe_id",
                "idx_item_cafe_participacao_cafe"
        );
    }

    @Test
    void duplicateCpfIsRejected() {
        insertColaborador("Joao", "12345678901");

        assertConstraintViolation("uk_colaborador_cpf",
                () -> insertColaborador("Maria", "12345678901"));
    }

    @Test
    void nonNormalizedCpfIsRejected() {
        assertConstraintViolation("ck_colaborador_cpf_formato",
                () -> insertColaborador("Joao", "1234567890A"));
    }

    @Test
    void duplicateBreakfastDateIsRejected() {
        insertCafe(LocalDate.of(2026, 9, 1));

        assertConstraintViolation("uk_cafe_da_manha_data",
                () -> insertCafe(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void breakfastHasNoItemQuantityColumn() {
        Integer columns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'cafe_da_manha'
                  AND column_name = 'quantidade_itens_por_colaborador'
                """, Integer.class);

        assertThat(columns).isZero();
    }

    @Test
    void collaboratorCanParticipateOnlyOnceInTheSameBreakfast() {
        long colaboradorId = insertColaborador("Joao", "12345678901");
        long cafeId = insertCafe(LocalDate.of(2026, 9, 1));
        insertParticipacao(colaboradorId, cafeId);

        assertConstraintViolation("uk_participacao_colaborador_cafe",
                () -> insertParticipacao(colaboradorId, cafeId));
    }

    @Test
    void normalizedItemNameIsUniqueInsideTheSameBreakfast() {
        long cafeId = insertCafe(LocalDate.of(2026, 9, 1));
        long joaoId = insertColaborador("Joao", "12345678901");
        long mariaId = insertColaborador("Maria", "10987654321");
        long participacaoJoao = insertParticipacao(joaoId, cafeId);
        long participacaoMaria = insertParticipacao(mariaId, cafeId);
        insertItem(participacaoJoao, cafeId, "Queijo", "queijo");

        assertConstraintViolation("uk_item_cafe_cafe_nome_normalizado",
                () -> insertItem(participacaoMaria, cafeId, " queijo ", "queijo"));
    }

    @Test
    void sameNormalizedItemNameIsAllowedInDifferentBreakfasts() {
        long colaboradorId = insertColaborador("Maria", "10987654321");
        long primeiroCafeId = insertCafe(LocalDate.of(2026, 9, 1));
        long segundoCafeId = insertCafe(LocalDate.of(2026, 9, 3));
        long primeiraParticipacao = insertParticipacao(colaboradorId, primeiroCafeId);
        long segundaParticipacao = insertParticipacao(colaboradorId, segundoCafeId);

        insertItem(primeiraParticipacao, primeiroCafeId, "Queijo", "queijo");
        insertItem(segundaParticipacao, segundoCafeId, "QUEIJO", "queijo");

        assertThat(countRows("item_cafe")).isEqualTo(2);
    }

    @Test
    void invalidStatusIsRejectedAndDefaultStatusIsPending() {
        long colaboradorId = insertColaborador("Joao", "12345678901");
        long cafeId = insertCafe(LocalDate.of(2026, 9, 1));
        long participacaoId = insertParticipacao(colaboradorId, cafeId);
        long itemId = insertItem(participacaoId, cafeId, "Queijo", "queijo");

        String status = jdbc.queryForObject(
                "SELECT status FROM item_cafe WHERE id = ?",
                String.class,
                itemId
        );
        assertThat(status).isEqualTo("PENDENTE");

        assertConstraintViolation("ck_item_cafe_status", () -> jdbc.update("""
                INSERT INTO item_cafe (participacao_id, cafe_id, nome, nome_normalizado, status)
                VALUES (?, ?, 'Cafe', 'cafe', 'INVALIDO')
                """, participacaoId, cafeId));
    }

    @Test
    void foreignKeysRejectInvalidReferences() {
        long colaboradorId = insertColaborador("Joao", "12345678901");
        long cafeId = insertCafe(LocalDate.of(2026, 9, 1));

        assertConstraintViolation("fk_participacao_colaborador",
                () -> insertParticipacao(-1L, cafeId));
        assertConstraintViolation("fk_participacao_cafe",
                () -> insertParticipacao(colaboradorId, -1L));
        assertConstraintViolation("fk_item_cafe_participacao",
                () -> insertItem(-1L, cafeId, "Queijo", "queijo"));
    }

    @Test
    void itemCannotReferenceParticipationFromAnotherBreakfast() {
        long colaboradorId = insertColaborador("Joao", "12345678901");
        long primeiroCafeId = insertCafe(LocalDate.of(2026, 9, 1));
        long segundoCafeId = insertCafe(LocalDate.of(2026, 9, 3));
        long participacaoId = insertParticipacao(colaboradorId, primeiroCafeId);

        assertConstraintViolation("fk_item_cafe_participacao",
                () -> insertItem(participacaoId, segundoCafeId, "Queijo", "queijo"));
    }

    @Test
    void deletingBreakfastOrCollaboratorCascadesToParticipationAndItems() {
        long colaboradorId = insertColaborador("Joao", "12345678901");
        long cafeId = insertCafe(LocalDate.of(2026, 9, 1));
        long participacaoId = insertParticipacao(colaboradorId, cafeId);
        insertItem(participacaoId, cafeId, "Queijo", "queijo");

        jdbc.update("DELETE FROM cafe_da_manha WHERE id = ?", cafeId);

        assertThat(countRows("participacao")).isZero();
        assertThat(countRows("item_cafe")).isZero();

        long outroCafeId = insertCafe(LocalDate.of(2026, 9, 3));
        long outraParticipacaoId = insertParticipacao(colaboradorId, outroCafeId);
        insertItem(outraParticipacaoId, outroCafeId, "Cafe", "cafe");

        jdbc.update("DELETE FROM colaborador WHERE id = ?", colaboradorId);

        assertThat(countRows("participacao")).isZero();
        assertThat(countRows("item_cafe")).isZero();
    }

    private long insertColaborador(String nome, String cpf) {
        return jdbc.queryForObject("""
                INSERT INTO colaborador (nome, cpf)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, nome, cpf);
    }

    private long insertCafe(LocalDate data) {
        return jdbc.queryForObject("""
                INSERT INTO cafe_da_manha (data)
                VALUES (?)
                RETURNING id
                """, Long.class, data);
    }

    private long insertParticipacao(long colaboradorId, long cafeId) {
        return jdbc.queryForObject("""
                INSERT INTO participacao (colaborador_id, cafe_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, colaboradorId, cafeId);
    }

    private long insertItem(long participacaoId, long cafeId, String nome, String nomeNormalizado) {
        return jdbc.queryForObject("""
                INSERT INTO item_cafe (participacao_id, cafe_id, nome, nome_normalizado)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Long.class, participacaoId, cafeId, nome, nomeNormalizado);
    }

    private int countRows(String table) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private void assertConstraintViolation(String constraintName, Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraintName);
    }
}
