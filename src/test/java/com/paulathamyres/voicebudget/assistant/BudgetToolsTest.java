package com.paulathamyres.voicebudget.assistant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import com.paulathamyres.voicebudget.transaction.Category;
import com.paulathamyres.voicebudget.transaction.NewTransaction;
import com.paulathamyres.voicebudget.transaction.TransactionService;
import com.paulathamyres.voicebudget.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetToolsTest {

    @Mock
    private TransactionService transactionService;

    private BudgetTools tools;

    @BeforeEach
    void setUp() {
        tools = new BudgetTools(transactionService);
    }

    @Test
    @DisplayName("Repassa o que a IA entendeu para o serviço de movimentações")
    void registersTransactionThroughService() {
        tools.registerTransaction("mercado", new BigDecimal("45"), TransactionType.EXPENSE, Category.FOOD, null);

        verify(transactionService).register(
                new NewTransaction("mercado", new BigDecimal("45"), TransactionType.EXPENSE, Category.FOOD, null));
    }

    @Test
    @DisplayName("Sem mês informado, resume o mês atual")
    void summarizesCurrentMonthWhenOmitted() {
        YearMonth november = YearMonth.of(2026, 11);
        when(transactionService.currentMonth()).thenReturn(november);

        tools.getMonthlySummary(null);

        verify(transactionService).summarize(november);
    }

    @Test
    @DisplayName("Entende o mês no formato AAAA-MM")
    void parsesMonth() {
        tools.getMonthlySummary("2026-10");

        verify(transactionService).summarize(YearMonth.of(2026, 10));
    }

    @Test
    @DisplayName("Explica o formato quando o mês vem inválido")
    void rejectsInvalidMonth() {
        assertThatThrownBy(() -> tools.getMonthlySummary("outubro"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("AAAA-MM");
    }

    @Test
    @DisplayName("Usa 5 como padrão e no máximo 20 movimentações recentes")
    void limitsRecentTransactions() {
        tools.listRecentTransactions(null);
        tools.listRecentTransactions(100);

        verify(transactionService).findRecent(BudgetTools.DEFAULT_RECENT_LIMIT);
        verify(transactionService).findRecent(BudgetTools.MAX_RECENT_LIMIT);
    }
}
