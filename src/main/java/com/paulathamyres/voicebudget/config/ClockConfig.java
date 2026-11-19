package com.paulathamyres.voicebudget.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock(BudgetProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
