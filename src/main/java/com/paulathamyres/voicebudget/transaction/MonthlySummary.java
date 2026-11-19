package com.paulathamyres.voicebudget.transaction;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummary(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        List<CategoryTotal> expensesByCategory) {

    public record CategoryTotal(Category category, BigDecimal total) {
    }
}
