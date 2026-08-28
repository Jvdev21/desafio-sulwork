package com.sulwork.desafio;

import com.sulwork.desafio.domain.model.ItemCafeStatus;
import com.sulwork.desafio.dto.request.CafeCreateRequest;
import com.sulwork.desafio.dto.request.CafeUpdateRequest;
import com.sulwork.desafio.dto.request.ItemCreateRequest;
import com.sulwork.desafio.dto.request.ItemUpdateRequest;
import com.sulwork.desafio.dto.request.ParticipacaoCreateRequest;
import com.sulwork.desafio.dto.response.CafeResponse;
import com.sulwork.desafio.dto.response.ItemResponse;
import com.sulwork.desafio.dto.response.ParticipacaoResponse;
import com.sulwork.desafio.exception.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CafeParticipacaoItemApiIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("desafio_sulwork_domain_test")
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
    void createsCafeWithFutureDate() {
        LocalDate date = futureDate(10);
        ResponseEntity<CafeResponse> response = createCafe(date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(date);
        assertThat(response.getBody().createdAt()).isNotNull();
    }

    @Test
    void rejectsCafeToday() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                cafesUrl(), new CafeCreateRequest(LocalDate.now()), ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("A data do café deve ser maior que a data atual.");
    }

    @Test
    void rejectsCafeInPast() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                cafesUrl(), new CafeCreateRequest(LocalDate.now().minusDays(1)), ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsDuplicateCafeDate() {
        LocalDate date = futureDate(11);
        createCafe(date);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                cafesUrl(), new CafeCreateRequest(date), ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Já existe um café cadastrado para esta data.");
    }

    @Test
    void listsCafesOrderedByDate() {
        createCafe(futureDate(20));
        createCafe(futureDate(15));

        ResponseEntity<List<CafeResponse>> response = restTemplate.exchange(
                cafesUrl(), HttpMethod.GET, null, new ParameterizedTypeReference<>() { }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(CafeResponse::data)
                .containsExactly(futureDate(15), futureDate(20));
    }

    @Test
    void findsCafeById() {
        CafeResponse cafe = createCafe(futureDate(12)).getBody();

        ResponseEntity<CafeResponse> response = restTemplate.getForEntity(
                cafesUrl() + "/" + cafe.id(), CafeResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(cafe.data());
    }

    @Test
    void updatesCafe() {
        CafeResponse cafe = createCafe(futureDate(12)).getBody();
        LocalDate updatedDate = futureDate(13);

        ResponseEntity<CafeResponse> response = restTemplate.exchange(
                cafesUrl() + "/" + cafe.id(), HttpMethod.PUT,
                new HttpEntity<>(new CafeUpdateRequest(updatedDate)), CafeResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(updatedDate);
    }

    @Test
    void deletesCafe() {
        CafeResponse cafe = createCafe(futureDate(12)).getBody();

        ResponseEntity<Void> response = restTemplate.exchange(
                cafesUrl() + "/" + cafe.id(), HttpMethod.DELETE, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cafe_da_manha", Long.class)).isZero();
    }

    @Test
    void returnsNotFoundForMissingCafe() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                cafesUrl() + "/999", ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Café da manhã não encontrado.");
    }

    @Test
    void createsParticipationWithOneItem() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ParticipacaoResponse> response = createParticipation(
                cafeId, collaboratorId, List.of("Bolo")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().itens()).singleElement()
                .satisfies(item -> {
                    assertThat(item.nome()).isEqualTo("Bolo");
                    assertThat(item.status()).isEqualTo(ItemCafeStatus.PENDENTE);
                });
    }

    @Test
    void createsParticipationWithMultipleItems() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ParticipacaoResponse> response = createParticipation(
                cafeId, collaboratorId, List.of("Bolo", "Suco de Acerola")
        );

        assertThat(response.getBody().itens()).extracting(ItemResponse::nome)
                .containsExactly("Bolo", "Suco de Acerola");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isEqualTo(1);
    }

    @Test
    void createsParticipationWithFiveItems() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ParticipacaoResponse> response = createParticipation(
                cafeId, collaboratorId, List.of("Café", "Água", "Pão", "Bolo", "Frutas")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().itens()).extracting(ItemResponse::nome)
                .containsExactly("Café", "Água", "Pão", "Bolo", "Frutas");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isOne();
    }

    @Test
    void allowsManyDifferentItemsInOneParticipation() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ParticipacaoResponse> response = createParticipation(
        cafeId, collaboratorId, List.of("Café", "Pão", "Bolo", "Suco", "Leite", "Queijo", "Guardanapos")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().itens()).extracting(ItemResponse::nome)
                .containsExactly("Café", "Pão", "Bolo", "Suco", "Leite", "Queijo", "Guardanapos");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isOne();
    }

    @Test
    void rejectsParticipationWithoutItems() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(collaboratorId, List.of()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fields())
                .containsEntry("itens", "A participação deve possuir pelo menos um item.");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isZero();
    }

    @Test
    void rejectsDuplicateParticipation() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");
        createParticipation(cafeId, collaboratorId, List.of("Bolo"));

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(collaboratorId, List.of("Suco")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Colaborador já participa deste café.");
    }

    @Test
    void rejectsMissingCollaborator() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(999L, List.of("Bolo")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Colaborador não encontrado.");
    }

    @Test
    void rejectsMissingCafeWhenCreatingParticipation() {
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(999L),
                new ParticipacaoCreateRequest(collaboratorId, List.of("Bolo")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Café da manhã não encontrado.");
    }

    @Test
    void rollsBackParticipationWhenAnItemIsDuplicated() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long collaboratorId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(collaboratorId, List.of("Pão de queijo", " PAO   DE QUEIJO ")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_cafe", Long.class)).isZero();
    }

    @Test
    void listsCafeParticipantsWithTheirItems() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        createParticipation(cafeId, mariaId, List.of("Bolo", "Suco"));

        ResponseEntity<List<ParticipacaoResponse>> response = restTemplate.exchange(
                participantsUrl(cafeId), HttpMethod.GET, null,
                new ParameterizedTypeReference<>() { }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).singleElement()
                .satisfies(participation -> assertThat(participation.itens()).hasSize(2));
    }

    @Test
    void removesParticipantAndCascadesItems() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        Long participationId = createParticipation(cafeId, mariaId, List.of("Bolo"))
                .getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                participantsUrl(cafeId) + "/" + participationId,
                HttpMethod.DELETE, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacao", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_cafe", Long.class)).isZero();
    }

    @Test
    void rejectsDuplicateItemInSameCafe() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        Long joaoId = insertCollaborator("João", "11144477735");
        createParticipation(cafeId, mariaId, List.of("Queijo"));

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(joaoId, List.of("QUEIJO")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message())
                .isEqualTo("A opção 'QUEIJO' já foi cadastrada para este café.");
    }

    @Test
    void treatsCaseWhitespaceAndAccentsAsEquivalent() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        Long participationId = createParticipation(cafeId, mariaId, List.of("Pão de Queijo"))
                .getBody().id();

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                participationsUrl() + "/" + participationId + "/itens",
                new ItemCreateRequest("  PAO   DE   QUEIJO  "), ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void allowsSameItemInAnotherCafe() {
        Long firstCafe = createCafe(futureDate(10)).getBody().id();
        Long secondCafe = createCafe(futureDate(11)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");

        ResponseEntity<ParticipacaoResponse> first = createParticipation(firstCafe, mariaId, List.of("Bolo"));
        ResponseEntity<ParticipacaoResponse> second = createParticipation(secondCafe, mariaId, List.of("BOLO"));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void addsItemToParticipation() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        ParticipacaoResponse participation = createParticipation(cafeId, mariaId, List.of("Bolo", "Pão")).getBody();
        Long participationId = participation.id();
        restTemplate.exchange(
                itemsUrl() + "/" + participation.itens().get(1).id(), HttpMethod.DELETE, null, Void.class
        );

        ResponseEntity<ItemResponse> response = restTemplate.postForEntity(
                participationsUrl() + "/" + participationId + "/itens",
                new ItemCreateRequest("Suco"), ItemResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().nome()).isEqualTo("Suco");
        assertThat(response.getBody().status()).isEqualTo(ItemCafeStatus.PENDENTE);
    }

    @Test
    void editsItemAndRecalculatesNormalizedName() {
        ItemResponse item = createItemForNewParticipation("Bolo");

        ResponseEntity<ItemResponse> response = restTemplate.exchange(
                itemsUrl() + "/" + item.id(), HttpMethod.PUT,
                new HttpEntity<>(new ItemUpdateRequest("Pão Doce")), ItemResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().nome()).isEqualTo("Pão Doce");
        assertThat(jdbc.queryForObject(
                "SELECT nome_normalizado FROM item_cafe WHERE id = ?", String.class, item.id()
        )).isEqualTo("pao doce");
    }

    @Test
    void rejectsDuplicateWhenEditingItem() {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        ParticipacaoResponse participation = createParticipation(
                cafeId, mariaId, List.of("Bolo", "Suco")
        ).getBody();
        Long juiceId = participation.itens().get(1).id();

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                itemsUrl() + "/" + juiceId, HttpMethod.PUT,
                new HttpEntity<>(new ItemUpdateRequest(" BÓLO ")), ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject(
                "SELECT nome FROM item_cafe WHERE id = ?", String.class, juiceId
        )).isEqualTo("Suco");
    }

    @Test
    void deletesItem() {
        ItemResponse item = createItemForNewParticipation("Bolo");

        ResponseEntity<Void> response = restTemplate.exchange(
                itemsUrl() + "/" + item.id(), HttpMethod.DELETE, null, Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_cafe", Long.class)).isZero();
    }

    private ItemResponse createItemForNewParticipation(String itemName) {
        Long cafeId = createCafe(futureDate(10)).getBody().id();
        Long mariaId = insertCollaborator("Maria", "52998224725");
        return createParticipation(cafeId, mariaId, List.of(itemName)).getBody().itens().get(0);
    }

    private ResponseEntity<CafeResponse> createCafe(LocalDate date) {
        return restTemplate.postForEntity(cafesUrl(), new CafeCreateRequest(date), CafeResponse.class);
    }

    private ResponseEntity<ParticipacaoResponse> createParticipation(
            Long cafeId, Long collaboratorId, List<String> items
    ) {
        return restTemplate.postForEntity(
                participantsUrl(cafeId),
                new ParticipacaoCreateRequest(collaboratorId, items),
                ParticipacaoResponse.class
        );
    }

    private Long insertCollaborator(String name, String cpf) {
        return jdbc.queryForObject(
                "INSERT INTO colaborador (nome, cpf) VALUES (?, ?) RETURNING id",
                Long.class, name, cpf
        );
    }

    private LocalDate futureDate(int days) {
        return LocalDate.now().plusDays(days);
    }

    private String cafesUrl() {
        return "http://localhost:" + port + "/api/cafes";
    }

    private String participantsUrl(Long cafeId) {
        return cafesUrl() + "/" + cafeId + "/participantes";
    }

    private String participationsUrl() {
        return "http://localhost:" + port + "/api/participacoes";
    }

    private String itemsUrl() {
        return "http://localhost:" + port + "/api/itens";
    }
}
