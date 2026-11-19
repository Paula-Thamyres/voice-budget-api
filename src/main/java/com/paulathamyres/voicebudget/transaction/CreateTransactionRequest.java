package com.paulathamyres.voicebudget.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @Schema(example = "Mercado")
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres.")
        String description,

        @Schema(example = "150.90")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais.")
        BigDecimal amount,

        @NotNull(message = "Informe se é receita (INCOME) ou despesa (EXPENSE).")
        TransactionType type,

        @NotNull(message = "A categoria é obrigatória.")
        Category category,

        @Schema(description = "Opcional. Sem data, vale o dia de hoje.", example = "2026-11-19")
        @PastOrPresent(message = "A data não pode estar no futuro.")
        LocalDate occurredOn) {

    NewTransaction toCommand() {
        return new NewTransaction(description, amount, type, category, occurredOn);
    }
}
