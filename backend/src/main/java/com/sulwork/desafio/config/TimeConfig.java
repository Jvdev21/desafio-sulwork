package com.sulwork.desafio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock applicationClock() {
        return Clock.system(APPLICATION_TIME_ZONE);
    }
}
