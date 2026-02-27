package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public Chat save(ChatMessage chatDto) {
        Chat chat = Chat.builder()
                .message(chatDto.getMessage())
                .writerUid(chatDto.getWriterUid())
                .sendTime(chatDto.getSendTime())
                .replyToId(chatDto.getReplyToId())
                .replyToMessage(chatDto.getReplyToMessage())
                .replyToUid(chatDto.getReplyToUid())
                .build();

        return chatRepository.save(chat);
    }
}
