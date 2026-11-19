package com.paulathamyres.voicebudget.transaction;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import com.paulathamyres.voicebudget.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    static final int MAX_DESCRIPTION_LENGTH = 120;
    static final int MAX_RECENT_LIMIT = 50;
    static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse register(NewTransaction command) {
        String description = validDescription(command.description());
        BigDecimal amount = validAmount(command.amount());
        TransactionType type = required(command.type(), "Informe se é receita ou despesa.");
        Category category = required(command.category(), "Informe a categoria da movimentação.");
        LocalDate occurredOn = validDate(command.occurredOn());

        Transaction transaction = new Transaction(description, amount, type, category, occurredOn, clock.instant());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return transactionRepository.findById(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByMonth(YearMonth month) {
        return transactionsOf(month).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findRecent(int limit) {
        if (limit < 1 || limit > MAX_RECENT_LIMIT) {
            throw new BusinessRuleException("A quantidade deve ficar entre 1 e %d.".formatted(MAX_RECENT_LIMIT));
        }
        return transactionRepository.findAllByOrderByOccurredOnDescIdDesc(Limit.of(limit)).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonthlySummary summarize(YearMonth month) {
        List<Transaction> transactions = transactionsOf(month);

        BigDecimal totalIncome = sum(transactions.stream().filter(t -> !t.isExpense()).toList());
        List<Transaction> expenses = transactions.stream().filter(Transaction::isExpense).toList();
        BigDecimal totalExpenses = sum(expenses);

        return new MonthlySummary(
                month.toString(),
                totalIncome,
                totalExpenses,
                totalIncome.subtract(totalExpenses),
                totalsByCategory(expenses));
    }

    @Transactional
    public void delete(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> notFound(id));
        transactionRepository.delete(transaction);
    }

    public YearMonth currentMonth() {
        return YearMonth.now(clock);
    }

    private List<Transaction> transactionsOf(YearMonth month) {
        return transactionRepository.findByOccurredOnBetweenOrderByOccurredOnDescIdDesc(
                month.atDay(1), month.atEndOfMonth());
    }

    private static List<MonthlySummary.CategoryTotal> totalsByCategory(List<Transaction> expenses) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        expenses.forEach(t -> totals.merge(t.getCategory(), t.getAmount(), BigDecimal::add));
        return totals.entrySet().stream()
                .map(entry -> new MonthlySummary.CategoryTotal(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MonthlySummary.CategoryTotal::total).reversed())
                .toList();
    }

    private static BigDecimal sum(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private static String validDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException("Informe uma descrição para a movimentação.");
        }
        String stripped = description.strip();
        if (stripped.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException(
                    "A descrição deve ter no máximo %d caracteres.".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return stripped;
    }

    private static BigDecimal validAmount(BigDecimal amount) {
        BigDecimal rounded = required(amount, "Informe o valor da movimentação.")
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        if (rounded.signum() <= 0) {
            throw new BusinessRuleException("O valor deve ser maior que zero.");
        }
        if (rounded.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("O valor está acima do limite permitido.");
        }
        return rounded;
    }

    private LocalDate validDate(LocalDate occurredOn) {
        LocalDate today = LocalDate.now(clock);
        if (occurredOn == null) {
            return today;
        }
        if (occurredOn.isAfter(today)) {
            throw new BusinessRuleException("A data não pode estar no futuro.");
        }
        return occurredOn;
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new BusinessRuleException(message);
        }
        return value;
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Movimentação não encontrada: " + id);
    }
}
