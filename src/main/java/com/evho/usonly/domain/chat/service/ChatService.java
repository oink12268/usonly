package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.dto.ChatDto;
import com.evho.usonly.domain.chat.model.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public void save(ChatDto chatDto) {
        Chat chat = Chat.builder()
                .message(chatDto.getMessage())
                .writerUid(chatDto.getWriterUid())
                .sendTime(chatDto.getSendTime())
                .build();

        chatRepository.save(chat);
    }
}
