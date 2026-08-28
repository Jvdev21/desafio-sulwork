package com.sulwork.desafio.validation.item;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ItemNameNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

    private ItemNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String collapsedWhitespace = REPEATED_WHITESPACE.matcher(value.trim()).replaceAll(" ");
        String lowercaseValue = collapsedWhitespace.toLowerCase(Locale.ROOT);
        String decomposedValue = Normalizer.normalize(lowercaseValue, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposedValue).replaceAll("");
    }
}
