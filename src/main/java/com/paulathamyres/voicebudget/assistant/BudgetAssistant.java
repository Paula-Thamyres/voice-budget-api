package com.paulathamyres.voicebudget.assistant;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class BudgetAssistant {

    static final String CONVERSATION_ID = "budget-assistant";

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd", Locale.forLanguageTag("pt-BR"));

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de orçamento pessoal que conversa em português do Brasil.
            Hoje é {today}. Use essa data para entender "hoje", "ontem" e o mês atual.

            Regras:
            - Use as ferramentas para registrar receitas e despesas e para consultar o resumo do mês
              ou as últimas movimentações. Nunca invente valores nem movimentações.
            - Se faltar o valor, ou se não estiver claro se é receita ou despesa, pergunte antes de registrar.
            - Depois de registrar, confirme o que foi salvo, com o valor em reais.
            - A resposta pode ser convertida em voz: responda em no máximo duas frases curtas,
              sem listas, sem markdown e sem emojis.
            """;

    private final ChatClient chatClient;
    private final BudgetTools budgetTools;
    private final Clock clock;

    public BudgetAssistant(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, BudgetTools budgetTools,
                           Clock clock) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.budgetTools = budgetTools;
        this.clock = clock;
    }

    public String reply(String userMessage) {
        String reply = chatClient.prompt()
                .system(system -> system.text(SYSTEM_PROMPT).param("today", TODAY_FORMAT.format(LocalDate.now(clock))))
                .user(userMessage)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                .tools(budgetTools)
                .call()
                .content();
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("The model returned an empty reply");
        }
        return reply.strip();
    }
}
