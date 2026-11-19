package com.paulathamyres.voicebudget.config;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("budget")
public record BudgetProperties(@NotNull ZoneId timeZone) {
}
