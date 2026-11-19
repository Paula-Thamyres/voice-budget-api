package com.paulathamyres.voicebudget.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "interactions")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(nullable = false)
    private String userMessage;

    @Column(nullable = false)
    private String reply;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Interaction() {
    }

    public Interaction(Channel channel, String userMessage, String reply, Instant createdAt) {
        this.channel = channel;
        this.userMessage = userMessage;
        this.reply = reply;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getReply() {
        return reply;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
