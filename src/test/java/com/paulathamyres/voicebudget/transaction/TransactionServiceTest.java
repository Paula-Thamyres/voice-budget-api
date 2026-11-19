package com.paulathamyres.voicebudget.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, CLOCK);
    }

    @Test
    @DisplayName("Usa a data de hoje e arredonda o valor para centavos")
    void registersWithTodayAndRoundedAmount() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.register(
                new NewTransaction("  Mercado ", new BigDecimal("45.005"), TransactionType.EXPENSE, Category.FOOD, null));

        assertThat(response.description()).isEqualTo("Mercado");
        assertThat(response.amount()).isEqualTo(new BigDecimal("45.00"));
        assertThat(response.occurredOn()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("Recusa valor zero ou negativo")
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.register(
                new NewTransaction("Mercado", new BigDecimal("0.001"), TransactionType.EXPENSE, Category.FOOD, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O valor deve ser maior que zero.");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recusa data no futuro")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> service.register(new NewTransaction(
                "Mercado", BigDecimal.TEN, TransactionType.EXPENSE, Category.FOOD, TODAY.plusDays(1))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A data não pode estar no futuro.");
    }

    @Test
    @DisplayName("Recusa movimentação sem tipo, que pode chegar da IA sem passar pela validação da API")
    void rejectsMissingType() {
        assertThatThrownBy(() -> service.register(
                new NewTransaction("Mercado", BigDecimal.TEN, null, Category.FOOD, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Informe se é receita ou despesa.");
    }

    @Test
    @DisplayName("Calcula saldo e ordena as categorias pelo maior gasto")
    void summarizesMonth() {
        YearMonth november = YearMonth.of(2026, 11);
        when(transactionRepository.findByOccurredOnBetweenOrderByOccurredOnDescIdDesc(
                november.atDay(1), november.atEndOfMonth()))
                .thenReturn(List.of(
                        transaction("3000.00", TransactionType.INCOME, Category.SALARY),
                        transaction("80.00", TransactionType.EXPENSE, Category.FOOD),
                        transaction("1200.00", TransactionType.EXPENSE, Category.HOUSING),
                        transaction("20.00", TransactionType.EXPENSE, Category.FOOD)));

        MonthlySummary summary = service.summarize(november);

        assertThat(summary.month()).isEqualTo("2026-11");
        assertThat(summary.totalIncome()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(summary.totalExpenses()).isEqualTo(new BigDecimal("1300.00"));
        assertThat(summary.balance()).isEqualTo(new BigDecimal("1700.00"));
        assertThat(summary.expensesByCategory())
                .extracting(MonthlySummary.CategoryTotal::category)
                .containsExactly(Category.HOUSING, Category.FOOD);
        assertThat(summary.expensesByCategory().get(1).total()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Mês sem movimentações tem tudo zerado")
    void summarizesEmptyMonth() {
        YearMonth month = YearMonth.of(2026, 1);
        when(transactionRepository.findByOccurredOnBetweenOrderByOccurredOnDescIdDesc(
                month.atDay(1), month.atEndOfMonth())).thenReturn(List.of());

        MonthlySummary summary = service.summarize(month);

        assertThat(summary.balance()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.expensesByCategory()).isEmpty();
    }

    @Test
    @DisplayName("Recusa quantidade fora do limite ao listar as recentes")
    void rejectsRecentLimitOutOfRange() {
        assertThatThrownBy(() -> service.findRecent(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findRecent(TransactionService.MAX_RECENT_LIMIT + 1))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static Transaction transaction(String amount, TransactionType type, Category category) {
        return new Transaction("Teste", new BigDecimal(amount), type, category, TODAY, CLOCK.instant());
    }
}
