package com.sulwork.desafio.validation.cpf;

public final class CpfValidator {

    private static final int CPF_LENGTH = 11;

    private CpfValidator() {
    }

    public static boolean isValid(String value) {
        final String cpf;
        try {
            cpf = CpfNormalizer.normalize(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        if (cpf == null || cpf.length() != CPF_LENGTH || hasRepeatedDigits(cpf)) {
            return false;
        }

        int firstCheckDigit = calculateCheckDigit(cpf, 9, 10);
        int secondCheckDigit = calculateCheckDigit(cpf, 10, 11);

        return Character.digit(cpf.charAt(9), 10) == firstCheckDigit
                && Character.digit(cpf.charAt(10), 10) == secondCheckDigit;
    }

    private static boolean hasRepeatedDigits(String cpf) {
        char firstDigit = cpf.charAt(0);
        return cpf.chars().allMatch(digit -> digit == firstDigit);
    }

    private static int calculateCheckDigit(String cpf, int digitCount, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < digitCount; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (initialWeight - index);
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
