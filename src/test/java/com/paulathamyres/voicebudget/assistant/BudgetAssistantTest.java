package com.paulathamyres.voicebudget.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.paulathamyres.voicebudget.transaction.TransactionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class BudgetAssistantTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private RecordingChatModel chatModel;
    private BudgetAssistant assistant;

    @BeforeEach
    void setUp() {
        chatModel = new RecordingChatModel("  Pronto, registrei 45 reais com mercado.  ");
        assistant = new BudgetAssistant(
                ChatClient.builder(chatModel),
                MessageWindowChatMemory.builder().build(),
                new BudgetTools(mock(TransactionService.class)),
                CLOCK);
    }

    @Test
    @DisplayName("Informa a data de hoje para a IA entender \"hoje\" e \"ontem\"")
    void sendsTodayInSystemPrompt() {
        assistant.reply("Gastei 45 reais no mercado");

        Prompt prompt = chatModel.prompts.getFirst();
        assertThat(prompt.getSystemMessage().getText()).contains("quinta-feira, 2026-11-19");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("Gastei 45 reais no mercado");
    }

    @Test
    @DisplayName("Devolve a resposta da IA sem espaços sobrando")
    void returnsStrippedReply() {
        assertThat(assistant.reply("Quanto gastei este mês?")).isEqualTo("Pronto, registrei 45 reais com mercado.");
    }

    @Test
    @DisplayName("Lembra da conversa anterior para entender respostas curtas como \"foi 45 reais\"")
    void keepsConversationContext() {
        assistant.reply("Gastei no mercado");
        assistant.reply("Foi 45 reais");

        List<Message> secondCall = chatModel.prompts.get(1).getInstructions();
        assertThat(secondCall)
                .extracting(Message::getText)
                .contains("Gastei no mercado", "Foi 45 reais");
    }

    private static final class RecordingChatModel implements ChatModel {

        private final String reply;
        private final List<Prompt> prompts = new ArrayList<>();

        private RecordingChatModel(String reply) {
            this.reply = reply;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
        }
    }
}
