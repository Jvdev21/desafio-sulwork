package com.sulwork.desafio.validation;

import com.sulwork.desafio.dto.request.ColaboradorCreateRequest;
import com.sulwork.desafio.validation.item.ValidItemName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BeanValidationTests {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidCollaboratorRequestWithMaskedCpf() {
        ColaboradorCreateRequest request = new ColaboradorCreateRequest("Maria da Silva", "529.982.247-25");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankOrOversizedCollaboratorNameAndInvalidCpf() {
        ColaboradorCreateRequest blankRequest = new ColaboradorCreateRequest("   ", "12345678901");
        ColaboradorCreateRequest oversizedRequest = new ColaboradorCreateRequest("a".repeat(151), "52998224725");

        assertThat(fieldsOf(validator.validate(blankRequest))).containsExactlyInAnyOrder("nome", "cpf");
        assertThat(fieldsOf(validator.validate(oversizedRequest))).containsExactly("nome");
    }

    @Test
    void itemNameValidationMatchesDatabaseLimit() {
        assertThat(validator.validate(new ItemNameHolder(null))).isNotEmpty();
        assertThat(validator.validate(new ItemNameHolder("   "))).isNotEmpty();
        assertThat(validator.validate(new ItemNameHolder("a".repeat(121)))).isNotEmpty();
        assertThat(validator.validate(new ItemNameHolder("Pão Francês"))).isEmpty();
    }

    private Set<String> fieldsOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private record ItemNameHolder(@ValidItemName String nome) {
    }
}
