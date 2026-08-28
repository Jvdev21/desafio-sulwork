package com.sulwork.desafio.validation.cpf;

import java.util.regex.Pattern;

public final class CpfNormalizer {

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final Pattern STANDARD_MASK = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

    private CpfNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return "";
        }
        if (ONLY_DIGITS.matcher(trimmedValue).matches()) {
            return trimmedValue;
        }
        if (STANDARD_MASK.matcher(trimmedValue).matches()) {
            return trimmedValue.replace(".", "").replace("-", "");
        }

        throw new IllegalArgumentException("CPF contém caracteres ou formato inválido.");
    }
}
