package com.paulathamyres.voicebudget.assistant;

import com.paulathamyres.voicebudget.shared.exception.BusinessRuleException;
import com.paulathamyres.voicebudget.shared.exception.ExternalServiceException;
import com.paulathamyres.voicebudget.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssistantService {

    static final Set<String> SUPPORTED_AUDIO_EXTENSIONS =
            Set.of("flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "ogg", "wav", "webm");
    static final int MAX_HISTORY_LIMIT = 50;

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final TranscriptionModel transcriptionModel;
    private final BudgetAssistant budgetAssistant;
    private final TextToSpeechModel textToSpeechModel;
    private final InteractionRepository interactionRepository;
    private final Clock clock;

    public AssistantService(TranscriptionModel transcriptionModel, BudgetAssistant budgetAssistant,
                            TextToSpeechModel textToSpeechModel, InteractionRepository interactionRepository,
                            Clock clock) {
        this.transcriptionModel = transcriptionModel;
        this.budgetAssistant = budgetAssistant;
        this.textToSpeechModel = textToSpeechModel;
        this.interactionRepository = interactionRepository;
        this.clock = clock;
    }

    public InteractionResponse handleVoice(MultipartFile audio) {
        validateAudio(audio);
        String transcription = callProvider("transcrever o áudio",
                () -> transcriptionModel.transcribe(audio.getResource()));
        if (transcription == null || transcription.isBlank()) {
            throw new BusinessRuleException("Não consegui entender o áudio. Tente gravar de novo, mais perto do microfone.");
        }
        return answer(Channel.VOICE, transcription.strip());
    }

    public InteractionResponse handleText(String message) {
        return answer(Channel.TEXT, message.strip());
    }

    public List<InteractionResponse> findRecent(int limit) {
        if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new BusinessRuleException("A quantidade deve ficar entre 1 e %d.".formatted(MAX_HISTORY_LIMIT));
        }
        return interactionRepository.findAllByOrderByCreatedAtDescIdDesc(Limit.of(limit)).stream()
                .map(InteractionResponse::from)
                .toList();
    }

    public InteractionResponse findById(Long id) {
        return InteractionResponse.from(findInteraction(id));
    }

    public byte[] speak(Long interactionId) {
        String reply = findInteraction(interactionId).getReply();
        return callProvider("gerar o áudio da resposta", () -> textToSpeechModel.call(reply));
    }

    private InteractionResponse answer(Channel channel, String userMessage) {
        String reply = callProvider("gerar a resposta", () -> budgetAssistant.reply(userMessage));
        Interaction interaction = new Interaction(channel, userMessage, reply, clock.instant());
        return InteractionResponse.from(interactionRepository.save(interaction));
    }

    private Interaction findInteraction(Long id) {
        return interactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interação não encontrada: " + id));
    }

    private static void validateAudio(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessRuleException("Envie um arquivo de áudio.");
        }
        String extension = StringUtils.getFilenameExtension(audio.getOriginalFilename());
        if (extension == null || !SUPPORTED_AUDIO_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("Formato de áudio não suportado. Use um destes: "
                    + String.join(", ", SUPPORTED_AUDIO_EXTENSIONS.stream().sorted().toList()) + ".");
        }
    }

    private static <T> T callProvider(String operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("AI provider call failed while trying to {}", operation, ex);
            throw new ExternalServiceException(
                    "Não foi possível %s agora. Tente novamente em instantes.".formatted(operation), ex);
        }
    }
}
