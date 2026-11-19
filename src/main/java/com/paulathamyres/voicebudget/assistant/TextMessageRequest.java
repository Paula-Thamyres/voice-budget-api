package com.paulathamyres.voicebudget.assistant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextMessageRequest(

        @Schema(example = "Gastei 45 reais no mercado hoje")
        @NotBlank(message = "A mensagem é obrigatória.")
        @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres.")
        String message) {
}
