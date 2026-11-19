package com.paulathamyres.voicebudget.assistant;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import com.paulathamyres.voicebudget.transaction.Category;
import com.paulathamyres.voicebudget.transaction.MonthlySummary;
import com.paulathamyres.voicebudget.transaction.NewTransaction;
import com.paulathamyres.voicebudget.transaction.TransactionResponse;
import com.paulathamyres.voicebudget.transaction.TransactionService;
import com.paulathamyres.voicebudget.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class BudgetTools {

    static final int DEFAULT_RECENT_LIMIT = 5;
    static final int MAX_RECENT_LIMIT = 20;

    private final TransactionService transactionService;

    public BudgetTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Tool(description = "Registra uma receita ou uma despesa no orçamento pessoal.")
    public TransactionResponse registerTransaction(
            @ToolParam(description = "Descrição curta, por exemplo: mercado, aluguel, salário") String description,
            @ToolParam(description = "Valor em reais, positivo, usando ponto como separador decimal") BigDecimal amount,
            @ToolParam(description = "INCOME para dinheiro que entrou, EXPENSE para dinheiro que saiu") TransactionType type,
            @ToolParam(description = "FOOD (mercado, restaurante, lanche), TRANSPORT (combustível, ônibus, app), "
                    + "HOUSING (aluguel, luz, água, internet), HEALTH (farmácia, consulta), LEISURE (cinema, viagem), "
                    + "EDUCATION (curso, livro), SALARY (salário, freela) ou OTHER") Category category,
            @ToolParam(description = "Data no formato AAAA-MM-DD. Omita quando for hoje", required = false)
            LocalDate occurredOn) {
        return transactionService.register(new NewTransaction(description, amount, type, category, occurredOn));
    }

    @Tool(description = "Resumo de um mês: total de receitas, total de despesas, saldo e gastos por categoria.")
    public MonthlySummary getMonthlySummary(
            @ToolParam(description = "Mês no formato AAAA-MM. Omita para o mês atual", required = false) String month) {
        return transactionService.summarize(parseMonth(month));
    }

    @Tool(description = "Lista as movimentações mais recentes, da mais nova para a mais antiga.")
    public List<TransactionResponse> listRecentTransactions(
            @ToolParam(description = "Quantidade de movimentações, de 1 a 20. Omita para 5", required = false)
            Integer limit) {
        int effectiveLimit = limit != null ? Math.min(limit, MAX_RECENT_LIMIT) : DEFAULT_RECENT_LIMIT;
        return transactionService.findRecent(effectiveLimit);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return transactionService.currentMonth();
        }
        try {
            return YearMonth.parse(month.strip());
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("Mês inválido: use o formato AAAA-MM, por exemplo 2026-11.");
        }
    }
}
