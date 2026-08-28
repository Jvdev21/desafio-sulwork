package com.sulwork.desafio.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentDateProviderTests {

    @Test
    void usesInjectedClockToProvideCurrentDate() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-08-27T01:30:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );

        CurrentDateProvider provider = new CurrentDateProvider(fixedClock);

        assertThat(provider.today()).isEqualTo(LocalDate.of(2026, 8, 26));
    }
}
