package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatDto;
import com.evho.usonly.domain.chat.model.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.ChatService;
import com.evho.usonly.global.kafka.ChatNotificationProducer;
import com.evho.usonly.global.kafka.event.ChatNotificationEvent;
import com.evho.usonly.global.utils.FileUploadUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatNotificationProducer chatNotificationProducer;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    @PostMapping("/api/chat/image")
    public Map<String, String> uploadChatImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);

        String imageUrl = baseUrl + fileName;
        return Map.of("imageUrl", imageUrl);
    }

    @GetMapping("/api/chats")
    public List<Chat> getChats(
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int size) {
        List<Chat> chats;
        if (before == null) {
            chats = chatRepository.findByOrderByIdDesc(PageRequest.of(0, size));
        } else {
            chats = chatRepository.findByIdLessThanOrderByIdDesc(before, PageRequest.of(0, size));
        }
        List<Chat> result = new ArrayList<>(chats);
        java.util.Collections.reverse(result);
        return result;
    }

    @DeleteMapping("/api/chats/{id}")
    public void deleteChat(@PathVariable Long id,
                           @RequestAttribute("firebaseUid") String firebaseUid) {
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        if (!chat.getWriterUid().equals(firebaseUid)) {
            throw new IllegalStateException("본인 메시지만 삭제할 수 있습니다.");
        }
        chatRepository.deleteById(id);
        messagingTemplate.convertAndSend("/sub/chat/delete", Map.of("id", id));
    }

    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/sub/chat/typing", payload);
    }

    @MessageMapping("/chat")
    public void handleMessage(ChatDto request, java.security.Principal principal) {
        if (principal != null) {
            request.setWriterUid(principal.getName());
        }

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm"));
        request.setSendTime(formattedTime);

        Chat saved = chatService.save(request);

        messagingTemplate.convertAndSend("/sub/chat", saved);

        chatNotificationProducer.send(new ChatNotificationEvent(request.getWriterUid(), request.getMessage()));
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String writerUid;
    }
}
