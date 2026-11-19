package com.paulathamyres.voicebudget.assistant;

import java.time.Instant;

public record InteractionResponse(Long id, Channel channel, String userMessage, String reply, Instant createdAt) {

    public static InteractionResponse from(Interaction interaction) {
        return new InteractionResponse(
                interaction.getId(),
                interaction.getChannel(),
                interaction.getUserMessage(),
                interaction.getReply(),
                interaction.getCreatedAt());
    }
}
