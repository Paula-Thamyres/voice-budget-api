package com.paulathamyres.voicebudget.assistant;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/assistant")
@Tag(name = "Assistente", description = "Comandos por voz ou texto, com resposta em texto e em áudio")
public class AssistantController {

    static final String AUDIO_MPEG = "audio/mpeg";

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(path = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia um comando de voz, como \"gastei 45 reais no mercado\"")
    @ApiResponse(responseCode = "201", description = "Áudio transcrito e respondido")
    @ApiResponse(responseCode = "422", description = "Áudio vazio, em formato não suportado ou sem fala",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Serviço de IA indisponível",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<InteractionResponse> voice(
            @Parameter(description = "Áudio em m4a, mp3, wav, ogg, webm, flac ou mp4 (até 5 MB)")
            @RequestPart("audio") MultipartFile audio) {
        return created(assistantService.handleVoice(audio));
    }

    @PostMapping("/text")
    @Operation(summary = "Envia o mesmo tipo de comando por texto, sem gravar áudio")
    @ApiResponse(responseCode = "201", description = "Mensagem respondida")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Serviço de IA indisponível",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<InteractionResponse> text(@Valid @RequestBody TextMessageRequest request) {
        return created(assistantService.handleText(request.message()));
    }

    @GetMapping("/interactions")
    @Operation(summary = "Histórico das conversas com o assistente, da mais recente para a mais antiga")
    public List<InteractionResponse> findRecent(
            @Parameter(description = "Quantidade, de 1 a 50") @RequestParam(defaultValue = "20") int limit) {
        return assistantService.findRecent(limit);
    }

    @GetMapping("/interactions/{id}")
    @Operation(summary = "Busca uma interação pelo id")
    @ApiResponse(responseCode = "200", description = "Interação encontrada")
    @ApiResponse(responseCode = "404", description = "Interação não encontrada",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public InteractionResponse findById(@PathVariable Long id) {
        return assistantService.findById(id);
    }

    @GetMapping("/interactions/{id}/speech")
    @Operation(summary = "Devolve a resposta da interação em áudio (MP3)")
    @ApiResponse(responseCode = "200", description = "Áudio da resposta",
            content = @Content(mediaType = AUDIO_MPEG, schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Interação não encontrada",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Serviço de voz indisponível",
            content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<byte[]> speak(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(AUDIO_MPEG))
                .body(assistantService.speak(id));
    }

    private static ResponseEntity<InteractionResponse> created(InteractionResponse interaction) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/assistant/interactions/{id}")
                .buildAndExpand(interaction.id())
                .toUri();
        return ResponseEntity.created(location).body(interaction);
    }
}
