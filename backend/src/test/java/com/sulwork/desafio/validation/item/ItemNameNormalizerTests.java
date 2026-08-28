package com.sulwork.desafio.validation.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNameNormalizerTests {

    @Test
    void normalizesCaseAndOuterWhitespace() {
        assertThat(ItemNameNormalizer.normalize("Queijo")).isEqualTo("queijo");
        assertThat(ItemNameNormalizer.normalize(" QUEIJO ")).isEqualTo("queijo");
    }

    @Test
    void removesDiacriticsAndCollapsesInternalWhitespace() {
        assertThat(ItemNameNormalizer.normalize("Pão Francês")).isEqualTo("pao frances");
        assertThat(ItemNameNormalizer.normalize("PAO FRANCES")).isEqualTo("pao frances");
        assertThat(ItemNameNormalizer.normalize(" pão   francês ")).isEqualTo("pao frances");
        assertThat(ItemNameNormalizer.normalize("Café\tcom\nLeite")).isEqualTo("cafe com leite");
    }

    @Test
    void handlesNullAndBlankValuesPredictably() {
        assertThat(ItemNameNormalizer.normalize(null)).isNull();
        assertThat(ItemNameNormalizer.normalize("   ")).isEmpty();
    }
}
