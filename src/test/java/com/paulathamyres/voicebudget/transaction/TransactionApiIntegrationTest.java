package com.paulathamyres.voicebudget.transaction;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.paulathamyres.voicebudget.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionApiIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("Registra uma despesa, salva no PostgreSQL e devolve 201 com Location")
    void registersExpense() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(APPLICATION_JSON)
                        .content(transactionJson("Mercado", "150.90", "EXPENSE", "FOOD", "2025-01-10")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(150.90))
                .andExpect(jsonPath("$.occurredOn").value("2025-01-10"));
    }

    @Test
    @DisplayName("Lista só as movimentações do mês pedido")
    void listsTransactionsOfMonth() throws Exception {
        register("Padaria", "12.50", "EXPENSE", "FOOD", "2025-02-03");
        register("Cinema", "40.00", "EXPENSE", "LEISURE", "2025-03-01");

        mockMvc.perform(get("/transactions").param("month", "2025-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Padaria"));
    }

    @Test
    @DisplayName("Resume o mês com receitas, despesas, saldo e gastos por categoria")
    void summarizesMonth() throws Exception {
        register("Salário", "3000.00", "INCOME", "SALARY", "2025-04-05");
        register("Aluguel", "1200.00", "EXPENSE", "HOUSING", "2025-04-06");
        register("Mercado", "300.00", "EXPENSE", "FOOD", "2025-04-07");
        register("Feira", "50.00", "EXPENSE", "FOOD", "2025-04-08");

        mockMvc.perform(get("/transactions/summary").param("month", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses").value(1550.00))
                .andExpect(jsonPath("$.balance").value(1450.00))
                .andExpect(jsonPath("$.expensesByCategory[0].category").value("HOUSING"))
                .andExpect(jsonPath("$.expensesByCategory[1].category").value("FOOD"))
                .andExpect(jsonPath("$.expensesByCategory[1].total").value(350.00));
    }

    @Test
    @DisplayName("Devolve 400 com a lista de campos inválidos")
    void rejectsInvalidTransaction() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(APPLICATION_JSON)
                        .content(transactionJson("", "-5", "EXPENSE", "FOOD", "2025-01-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[1].field").value("description"));
    }

    @Test
    @DisplayName("Apaga uma movimentação e depois devolve 404 para ela")
    void deletesTransaction() throws Exception {
        long id = register("Lanche", "18.00", "EXPENSE", "FOOD", "2025-05-02");

        mockMvc.perform(delete("/transactions/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transactions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Movimentação não encontrada: " + id));
    }

    private long register(String description, String amount, String type, String category, String date)
            throws Exception {
        String json = mockMvc.perform(post("/transactions")
                        .contentType(APPLICATION_JSON)
                        .content(transactionJson(description, amount, type, category, date)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }

    private static String transactionJson(String description, String amount, String type, String category,
                                          String date) {
        return """
                {
                  "description": "%s",
                  "amount": %s,
                  "type": "%s",
                  "category": "%s",
                  "occurredOn": "%s"
                }
                """.formatted(description, amount, type, category, date);
    }
}
