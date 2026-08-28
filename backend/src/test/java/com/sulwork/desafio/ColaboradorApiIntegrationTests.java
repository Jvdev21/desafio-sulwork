package com.sulwork.desafio;

import com.sulwork.desafio.dto.request.ColaboradorCreateRequest;
import com.sulwork.desafio.dto.request.ColaboradorUpdateRequest;
import com.sulwork.desafio.dto.response.ColaboradorResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ColaboradorApiIntegrationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("desafio_sulwork_api_test")
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
        jdbc.execute("TRUNCATE TABLE colaborador RESTART IDENTITY CASCADE");
    }

    @Test
    void createsCollaboratorAndReturnsNormalizedCpf() {
        ResponseEntity<ColaboradorResponse> response = create("Maria Silva", "529.982.247-25");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString(url() + "/1");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().cpf()).isEqualTo("52998224725");
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
    }

    @Test
    void listsCollaboratorsAndAllowsRepeatedNames() {
        create("Alex Silva", "52998224725");
        create("Alex Silva", "11144477735");

        ResponseEntity<List<ColaboradorResponse>> response = restTemplate.exchange(
                url(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(ColaboradorResponse::nome)
                .containsExactly("Alex Silva", "Alex Silva");
    }

    @Test
    void findsCollaboratorById() {
        Long id = create("Maria", "52998224725").getBody().id();

        ResponseEntity<ColaboradorResponse> response = restTemplate.getForEntity(
                url() + "/" + id,
                ColaboradorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nome()).isEqualTo("Maria");
    }

    @Test
    void updatesCollaboratorAndAllowsKeepingOwnCpf() {
        Long id = create("Maria", "52998224725").getBody().id();

        ResponseEntity<ColaboradorResponse> response = restTemplate.exchange(
                url() + "/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(new ColaboradorUpdateRequest("Maria Souza", "529.982.247-25")),
                ColaboradorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nome()).isEqualTo("Maria Souza");
        assertThat(response.getBody().cpf()).isEqualTo("52998224725");
    }

    @Test
    void deletesCollaboratorAndActuallyRemovesRecord() {
        Long id = create("Maria", "52998224725").getBody().id();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                url() + "/" + id,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM colaborador WHERE id = ?",
                Long.class,
                id
        )).isZero();

        assertThat(restTemplate.getForEntity(url() + "/" + id, ApiErrorResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsDuplicateCpfWithConflict() {
        create("Maria", "52998224725");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                url(),
                new ColaboradorCreateRequest("João", "529.982.247-25"),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("CPF já cadastrado.");
    }

    @Test
    void rejectsInvalidCpfWithFieldValidationError() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                url(),
                new ColaboradorCreateRequest("Maria", "12345678901"),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).containsEntry("cpf", "CPF inválido.");
    }

    @Test
    void returnsNotFoundForMissingIds() {
        ResponseEntity<ApiErrorResponse> getResponse = restTemplate.getForEntity(
                url() + "/99",
                ApiErrorResponse.class
        );
        ResponseEntity<ApiErrorResponse> updateResponse = restTemplate.exchange(
                url() + "/99",
                HttpMethod.PUT,
                new HttpEntity<>(new ColaboradorUpdateRequest("Maria", "52998224725")),
                ApiErrorResponse.class
        );
        ResponseEntity<ApiErrorResponse> deleteResponse = restTemplate.exchange(
                url() + "/99",
                HttpMethod.DELETE,
                null,
                ApiErrorResponse.class
        );

        assertNotFound(getResponse);
        assertNotFound(updateResponse);
        assertNotFound(deleteResponse);
    }

    @Test
    void preventsUsingCpfOwnedByAnotherCollaborator() {
        Long mariaId = create("Maria", "52998224725").getBody().id();
        create("João", "11144477735");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                url() + "/" + mariaId,
                HttpMethod.PUT,
                new HttpEntity<>(new ColaboradorUpdateRequest("Maria", "111.444.777-35")),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("CPF já cadastrado.");
    }

    private ResponseEntity<ColaboradorResponse> create(String nome, String cpf) {
        return restTemplate.postForEntity(
                url(),
                new ColaboradorCreateRequest(nome, cpf),
                ColaboradorResponse.class
        );
    }

    private void assertNotFound(ResponseEntity<ApiErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Colaborador não encontrado.");
    }

    private String url() {
        return "http://localhost:" + port + "/api/colaboradores";
    }
}
