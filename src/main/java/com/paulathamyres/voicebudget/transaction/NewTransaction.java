package com.paulathamyres.voicebudget.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NewTransaction(
        String description,
        BigDecimal amount,
        TransactionType type,
        Category category,
        LocalDate occurredOn) {
}
