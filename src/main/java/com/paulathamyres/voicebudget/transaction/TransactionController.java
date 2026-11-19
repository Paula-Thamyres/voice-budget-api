package com.paulathamyres.voicebudget.transaction;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Movimentações", description = "Receitas e despesas do orçamento")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Registra uma receita ou despesa")
    @ApiResponse(responseCode = "201", description = "Movimentação registrada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TransactionResponse> register(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse created = transactionService.register(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista as movimentações de um mês")
    public List<TransactionResponse> findByMonth(
            @Parameter(description = "Mês no formato AAAA-MM. Sem ele, vale o mês atual.", example = "2026-11")
            @RequestParam(required = false) YearMonth month) {
        return transactionService.findByMonth(monthOrCurrent(month));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo do mês: receitas, despesas, saldo e gastos por categoria")
    public MonthlySummary summarize(
            @Parameter(description = "Mês no formato AAAA-MM. Sem ele, vale o mês atual.", example = "2026-11")
            @RequestParam(required = false) YearMonth month) {
        return transactionService.summarize(monthOrCurrent(month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma movimentação pelo id")
    @ApiResponse(responseCode = "200", description = "Movimentação encontrada")
    @ApiResponse(responseCode = "404", description = "Movimentação não encontrada",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public TransactionResponse findById(@PathVariable Long id) {
        return transactionService.findById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Apaga uma movimentação registrada por engano")
    @ApiResponse(responseCode = "204", description = "Movimentação apagada")
    @ApiResponse(responseCode = "404", description = "Movimentação não encontrada",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private YearMonth monthOrCurrent(YearMonth month) {
        return month != null ? month : transactionService.currentMonth();
    }
}
