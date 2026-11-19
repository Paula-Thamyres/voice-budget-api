package com.paulathamyres.voicebudget.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import com.paulathamyres.voicebudget.shared.exception.ExternalServiceException;
import com.paulathamyres.voicebudget.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final String COMMAND = "Gastei 45 reais no mercado";
    private static final String REPLY = "Pronto, registrei 45 reais com mercado.";

    @Mock
    private TranscriptionModel transcriptionModel;

    @Mock
    private BudgetAssistant budgetAssistant;

    @Mock
    private TextToSpeechModel textToSpeechModel;

    @Mock
    private InteractionRepository interactionRepository;

    private AssistantService service;

    @BeforeEach
    void setUp() {
        service = new AssistantService(transcriptionModel, budgetAssistant, textToSpeechModel, interactionRepository,
                CLOCK);
    }

    @Test
    @DisplayName("Transcreve, pede a resposta à IA e salva a interação de voz")
    void handlesVoiceCommand() {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("  " + COMMAND + " ");
        when(budgetAssistant.reply(COMMAND)).thenReturn(REPLY);
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InteractionResponse response = service.handleVoice(audio("comando.M4A"));

        ArgumentCaptor<Interaction> saved = ArgumentCaptor.forClass(Interaction.class);
        verify(interactionRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo(Channel.VOICE);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(CLOCK.instant());
        assertThat(response.userMessage()).isEqualTo(COMMAND);
        assertThat(response.reply()).isEqualTo(REPLY);
    }

    @Test
    @DisplayName("Recusa arquivo vazio")
    void rejectsEmptyAudio() {
        MockMultipartFile empty = new MockMultipartFile("audio", "comando.m4a", "audio/mp4", new byte[0]);

        assertThatThrownBy(() -> service.handleVoice(empty))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Envie um arquivo de áudio.");
        verifyNoInteractions(transcriptionModel);
    }

    @Test
    @DisplayName("Recusa formato que o serviço de transcrição não aceita")
    void rejectsUnsupportedFormat() {
        assertThatThrownBy(() -> service.handleVoice(audio("comando.aac")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("Formato de áudio não suportado");
        verifyNoInteractions(transcriptionModel);
    }

    @Test
    @DisplayName("Avisa quando o áudio não tem fala e não chama a IA")
    void rejectsSilentAudio() {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("   ");

        assertThatThrownBy(() -> service.handleVoice(audio("comando.m4a")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("Não consegui entender o áudio");
        verifyNoInteractions(budgetAssistant);
    }

    @Test
    @DisplayName("Traduz falha do provedor de IA em erro de serviço externo e não salva nada")
    void translatesProviderFailure() {
        when(budgetAssistant.reply(COMMAND)).thenThrow(new IllegalStateException("rate limit"));

        assertThatThrownBy(() -> service.handleText(COMMAND))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Não foi possível gerar a resposta agora. Tente novamente em instantes.");
        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gera o áudio a partir da resposta salva")
    void speaksSavedReply() {
        byte[] mp3 = {1, 2, 3};
        when(interactionRepository.findById(1L))
                .thenReturn(Optional.of(new Interaction(Channel.TEXT, COMMAND, REPLY, CLOCK.instant())));
        when(textToSpeechModel.call(REPLY)).thenReturn(mp3);

        assertThat(service.speak(1L)).isEqualTo(mp3);
    }

    @Test
    @DisplayName("Devolve 404 ao pedir o áudio de uma interação que não existe")
    void rejectsSpeechOfUnknownInteraction() {
        when(interactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.speak(99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(textToSpeechModel);
    }

    private static MockMultipartFile audio(String filename) {
        return new MockMultipartFile("audio", filename, "audio/mp4", new byte[] {1, 2, 3});
    }
}
