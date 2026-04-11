package com.evho.usonly.domain.chat.dto;

public record MigrationStatusResponse(boolean running, int progress, int total, boolean pineconeEnabled) {
}
