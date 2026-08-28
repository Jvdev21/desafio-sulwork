package com.sulwork.desafio;

import com.sulwork.desafio.domain.model.ItemCafeStatus;
import com.sulwork.desafio.dto.request.CafeUpdateRequest;
import com.sulwork.desafio.dto.request.ItemStatusUpdateRequest;
import com.sulwork.desafio.dto.request.ItemUpdateRequest;
import com.sulwork.desafio.dto.response.ItemResponse;
import com.sulwork.desafio.dto.response.ParticipacaoResponse;
import com.sulwork.desafio.exception.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ItemStatusApiIntegrationTests.FixedClockConfiguration.class)
class ItemStatusApiIntegrationTests {

    private static final LocalDate TODAY = LocalDate.of(2030, 6, 15);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("desafio_sulwork_status_test")
            .withUsername("desafio_sulwork")
            .withPassword("desafio_sulwork_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearDatabase() {
        jdbc.execute("TRUNCATE TABLE colaborador, cafe_da_manha RESTART IDENTITY CASCADE");
    }

    @Test
    void futureItemRemainsPending() {
        Fixture fixture = fixture(TODAY.plusDays(1), ItemCafeStatus.PENDENTE, "Bolo");

        ItemResponse item = getOnlyItem(fixture.cafeId());

        assertThat(item.status()).isEqualTo(ItemCafeStatus.PENDENTE);
    }

    @Test
    void rejectsStatusChangeBeforeCafeDate() {
        Fixture fixture = fixture(TODAY.plusDays(1), ItemCafeStatus.PENDENTE, "Bolo");

        ResponseEntity<ApiErrorResponse> response = patchStatus(fixture.itemId(), ItemCafeStatus.TROUXE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo("O status do item só pode ser alterado na data do café.");
        assertStoredStatus(fixture.itemId(), ItemCafeStatus.PENDENTE);
    }

    @Test
    void marksItemAsBroughtOnCafeDate() {
        Fixture fixture = fixture(TODAY, ItemCafeStatus.PENDENTE, "Bolo");

        ResponseEntity<ItemResponse> response = patchStatusSuccess(
                fixture.itemId(), ItemCafeStatus.TROUXE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(ItemCafeStatus.TROUXE);
        assertStoredStatus(fixture.itemId(), ItemCafeStatus.TROUXE);
    }

    @Test
    void marksItemAsNotBroughtOnCafeDate() {
        Fixture fixture = fixture(TODAY, ItemCafeStatus.PENDENTE, "Bolo");

        ResponseEntity<ItemResponse> response = patchStatusSuccess(
                fixture.itemId(), ItemCafeStatus.NAO_TROUXE
        );

        assertThat(response.getBody().status()).isEqualTo(ItemCafeStatus.NAO_TROUXE);
        assertStoredStatus(fixture.itemId(), ItemCafeStatus.NAO_TROUXE);
    }

    @Test
    void switchesBetweenBroughtAndNotBroughtOnCafeDate() {
        Fixture fixture = fixture(TODAY, ItemCafeStatus.TROUXE, "Bolo");

        assertThat(patchStatusSuccess(fixture.itemId(), ItemCafeStatus.NAO_TROUXE)
                .getBody().status()).isEqualTo(ItemCafeStatus.NAO_TROUXE);
        assertThat(patchStatusSuccess(fixture.itemId(), ItemCafeStatus.TROUXE)
                .getBody().status()).isEqualTo(ItemCafeStatus.TROUXE);
    }

    @Test
    void rejectsReturningToPendingOnCafeDate() {
        Fixture fixture = fixture(TODAY, ItemCafeStatus.TROUXE, "Bolo");

        ResponseEntity<ApiErrorResponse> response = patchStatus(
                fixture.itemId(), ItemCafeStatus.PENDENTE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("PENDENTE");
        assertStoredStatus(fixture.itemId(), ItemCafeStatus.TROUXE);
    }

    @Test
    void presentsPastPendingItemAsNotBrought() {
        Fixture fixture = fixture(TODAY.minusDays(1), ItemCafeStatus.PENDENTE, "Bolo");

        ItemResponse item = getOnlyItem(fixture.cafeId());

        assertThat(item.status()).isEqualTo(ItemCafeStatus.NAO_TROUXE);
    }

    @Test
    void getDoesNotSilentlyPersistEffectiveStatus() {
        Fixture fixture = fixture(TODAY.minusDays(1), ItemCafeStatus.PENDENTE, "Bolo");

        assertThat(getOnlyItem(fixture.cafeId()).status()).isEqualTo(ItemCafeStatus.NAO_TROUXE);

        assertStoredStatus(fixture.itemId(), ItemCafeStatus.PENDENTE);
    }

    @Test
    void rejectsStatusChangeAfterCafeDate() {
        Fixture fixture = fixture(TODAY.minusDays(1), ItemCafeStatus.PENDENTE, "Bolo");

        ResponseEntity<ApiErrorResponse> response = patchStatus(
                fixture.itemId(), ItemCafeStatus.TROUXE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .isEqualTo("O status do item não pode ser alterado após a data do café.");
        assertStoredStatus(fixture.itemId(), ItemCafeStatus.PENDENTE);
    }

    @Test
    void rejectsItemEditAndDeletionOnOrAfterCafeDate() {
        Fixture todayItem = fixture(TODAY, ItemCafeStatus.PENDENTE, "Bolo");
        Fixture pastItem = fixture(TODAY.minusDays(1), ItemCafeStatus.PENDENTE, "Suco");

        assertMutationBlocked(updateItem(todayItem.itemId(), "Bolo editado"));
        assertMutationBlocked(deleteItem(todayItem.itemId()));
        assertMutationBlocked(updateItem(pastItem.itemId(), "Suco editado"));
        assertMutationBlocked(deleteItem(pastItem.itemId()));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_cafe", Long.class)).isEqualTo(2L);
    }

    @Test
    void rejectsChangingOrDeletingCurrentAndPastCafes() {
        Long currentCafeId = insertCafe(TODAY);
        Long pastCafeId = insertCafe(TODAY.minusDays(1));

        assertCafeMutationBlocked(updateCafe(currentCafeId, TODAY.plusDays(5)));
        assertCafeMutationBlocked(deleteCafe(currentCafeId));
        assertCafeMutationBlocked(updateCafe(pastCafeId, TODAY.plusDays(6)));
        assertCafeMutationBlocked(deleteCafe(pastCafeId));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cafe_da_manha", Long.class)).isEqualTo(2L);
    }

    @Test
    void rejectsParticipationRemovalOnOrAfterCafeDate() {
        Fixture todayItem = fixture(TODAY, ItemCafeStatus.PENDENTE, "Bolo");
        Fixture pastItem = fixture(TODAY.minusDays(1), ItemCafeStatus.PENDENTE, "Suco");

        assertMutationBlocked(deleteParticipation(todayItem.cafeId(), todayItem.participationId()));
        assertMutationBlocked(deleteParticipation(pastItem.cafeId(), pastItem.participationId()));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isEqualTo(2L);
    }

    private Fixture fixture(LocalDate cafeDate, ItemCafeStatus status, String itemName) {
        Long cafeId = insertCafe(cafeDate);
        Long collaboratorId = jdbc.queryForObject(
                "INSERT INTO colaborador (nome, cpf) VALUES (?, ?) RETURNING id",
                Long.class,
                "Colaborador " + cafeId,
                cafeId == 1 ? "52998224725" : "11144477735"
        );
        Long participationId = jdbc.queryForObject(
                "INSERT INTO participacao (colaborador_id, cafe_id) VALUES (?, ?) RETURNING id",
                Long.class, collaboratorId, cafeId
        );
        Long itemId = jdbc.queryForObject(
                """
                INSERT INTO item_cafe (participacao_id, cafe_id, nome, nome_normalizado, status)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                participationId, cafeId, itemName, itemName.toLowerCase(), status.name()
        );
        return new Fixture(cafeId, participationId, itemId);
    }

    private Long insertCafe(LocalDate date) {
        return jdbc.queryForObject(
                "INSERT INTO cafe_da_manha (data) VALUES (?) RETURNING id",
                Long.class, date
        );
    }

    private ItemResponse getOnlyItem(Long cafeId) {
        ResponseEntity<List<ParticipacaoResponse>> response = restTemplate.exchange(
                cafesUrl() + "/" + cafeId + "/participantes",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() { }
        );
        return response.getBody().get(0).itens().get(0);
    }

    private ResponseEntity<ApiErrorResponse> patchStatus(Long itemId, ItemCafeStatus status) {
        return restTemplate.exchange(
                itemsUrl() + "/" + itemId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new ItemStatusUpdateRequest(status)),
                ApiErrorResponse.class
        );
    }

    private ResponseEntity<ItemResponse> patchStatusSuccess(Long itemId, ItemCafeStatus status) {
        return restTemplate.exchange(
                itemsUrl() + "/" + itemId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new ItemStatusUpdateRequest(status)),
                ItemResponse.class
        );
    }

    private ResponseEntity<ApiErrorResponse> updateItem(Long itemId, String name) {
        return restTemplate.exchange(
                itemsUrl() + "/" + itemId,
                HttpMethod.PUT,
                new HttpEntity<>(new ItemUpdateRequest(name)),
                ApiErrorResponse.class
        );
    }

    private ResponseEntity<ApiErrorResponse> deleteItem(Long itemId) {
        return restTemplate.exchange(
                itemsUrl() + "/" + itemId, HttpMethod.DELETE, null, ApiErrorResponse.class
        );
    }

    private ResponseEntity<ApiErrorResponse> updateCafe(Long cafeId, LocalDate date) {
        return restTemplate.exchange(
                cafesUrl() + "/" + cafeId,
                HttpMethod.PUT,
                new HttpEntity<>(new CafeUpdateRequest(date)),
                ApiErrorResponse.class
        );
    }

    private ResponseEntity<ApiErrorResponse> deleteCafe(Long cafeId) {
        return restTemplate.exchange(
                cafesUrl() + "/" + cafeId, HttpMethod.DELETE, null, ApiErrorResponse.class
        );
    }

    private ResponseEntity<ApiErrorResponse> deleteParticipation(Long cafeId, Long participationId) {
        return restTemplate.exchange(
                cafesUrl() + "/" + cafeId + "/participantes/" + participationId,
                HttpMethod.DELETE,
                null,
                ApiErrorResponse.class
        );
    }

    private void assertMutationBlocked(ResponseEntity<ApiErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("antes da data do café");
    }

    private void assertCafeMutationBlocked(ResponseEntity<ApiErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("dia ou passados");
    }

    private void assertStoredStatus(Long itemId, ItemCafeStatus expected) {
        assertThat(jdbc.queryForObject(
                "SELECT status FROM item_cafe WHERE id = ?", String.class, itemId
        )).isEqualTo(expected.name());
    }

    private String cafesUrl() {
        return "http://localhost:" + port + "/api/cafes";
    }

    private String itemsUrl() {
        return "http://localhost:" + port + "/api/itens";
    }

    private record Fixture(Long cafeId, Long participationId, Long itemId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2030-06-15T12:00:00Z"),
                    ZoneId.of("America/Sao_Paulo")
            );
        }
    }
}
