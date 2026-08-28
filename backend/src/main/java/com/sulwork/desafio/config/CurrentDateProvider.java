package com.sulwork.desafio.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class CurrentDateProvider {

    private final Clock clock;

    public CurrentDateProvider(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
