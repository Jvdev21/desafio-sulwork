package com.sulwork.desafio.validation.cpf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTests {

    @Test
    void acceptsValidCpfWithAndWithoutMask() {
        assertThat(CpfValidator.isValid("529.982.247-25")).isTrue();
        assertThat(CpfValidator.isValid("52998224725")).isTrue();
    }

    @Test
    void rejectsIncorrectLengthAndCheckDigits() {
        assertThat(CpfValidator.isValid("5299822472")).isFalse();
        assertThat(CpfValidator.isValid("52998224724")).isFalse();
    }

    @Test
    void rejectsAllRepeatedDigitSequences() {
        for (int digit = 0; digit <= 9; digit++) {
            assertThat(CpfValidator.isValid(String.valueOf(digit).repeat(11))).isFalse();
        }
    }

    @Test
    void rejectsNullEmptyAndInvalidCharacters() {
        assertThat(CpfValidator.isValid(null)).isFalse();
        assertThat(CpfValidator.isValid("")).isFalse();
        assertThat(CpfValidator.isValid("   ")).isFalse();
        assertThat(CpfValidator.isValid("abc529.982.247-25")).isFalse();
    }
}
