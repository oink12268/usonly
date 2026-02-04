package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatDto;
import com.evho.usonly.domain.chat.model.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {
    private final ChatRepository chatRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/api/chats")
    public List<Chat> getChats() {
        return chatRepository.findAllByOrderByCreatedAtAsc();
    }

    @MessageMapping("/chat")
    @SendTo("/sub/chat")
    public void handleMessage(ChatDto request) {

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm"));
        request.setSendTime(formattedTime);
//        Chat chat = Chat.builder()
//                .message(request.getMessage())
//                .writerUid(request.getWriterUid())
//                .
//                .build();
//
        chatService.save(request);

        System.out.println("메시지 받음: " + request.getMessage()); // 로그 확인용

        // (2) ★ [핵심] 구독자들에게 뿌리기 (Broadcasting)
        // "/sub/chat" 방을 듣고 있는 사람들에게 메시지를 쏴줍니다.
        messagingTemplate.convertAndSend("/sub/chat", request);
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String writerUid;
    }
}
