package com.sulwork.desafio.validation.cpf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CpfNormalizerTests {

    @Test
    void normalizesMaskedAndUnmaskedCpf() {
        assertThat(CpfNormalizer.normalize("732.442.160-13")).isEqualTo("73244216013");
        assertThat(CpfNormalizer.normalize("73244216013")).isEqualTo("73244216013");
        assertThat(CpfNormalizer.normalize(" 732.442.160-13 ")).isEqualTo("73244216013");
    }

    @Test
    void handlesNullAndEmptyValuesSafely() {
        assertThat(CpfNormalizer.normalize(null)).isNull();
        assertThat(CpfNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void rejectsLettersAndMalformedMasksInsteadOfSilentlyRemovingThem() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CpfNormalizer.normalize("abc732.442.160-13"))
                .withMessage("CPF contém caracteres ou formato inválido.");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CpfNormalizer.normalize("732.442.16013"));
    }
}
