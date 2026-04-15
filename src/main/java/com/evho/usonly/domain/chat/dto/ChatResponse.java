package com.evho.usonly.domain.chat.dto;

import com.evho.usonly.domain.chat.entity.Chat;
import java.time.LocalDateTime;

public record ChatResponse(
        Long id,
        String message,
        String writerUid,
        String sendTime,
        Long replyToId,
        String replyToMessage,
        String replyToUid,
        LocalDateTime createdAt
) {
    public static ChatResponse from(Chat chat) {
        return new ChatResponse(
                chat.getId(),
                chat.getMessage(),
                chat.getWriterUid(),
                chat.getSendTime(),
                chat.getReplyToId(),
                chat.getReplyToMessage(),
                chat.getReplyToUid(),
                chat.getCreatedAt()
        );
    }
}
