package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.AiChatSearchService;
import com.evho.usonly.domain.chat.service.ChatMigrationService;
import com.evho.usonly.domain.chat.service.ChatService;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.fcm.FcmService;
import com.evho.usonly.global.redis.RedisPublisher;
import com.evho.usonly.global.storage.FileStorageService;
import com.evho.usonly.global.utils.FileUploadUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatService chatService;
    private final RedisPublisher redisPublisher;
    private final AiChatSearchService aiChatSearchService;
    private final ChatMigrationService chatMigrationService;
    private final MemberService memberService;
    private final FcmService fcmService;
    private final FileStorageService fileStorageService;

    @PostMapping("/api/chat/image")
    public Map<String, String> uploadChatImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = fileStorageService.storeImage(file);
        return Map.of("imageUrl", imageUrl);
    }

    @PostMapping("/api/chat/file")
    public Map<String, String> uploadChatFile(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileUrl = fileStorageService.store(file, FileUploadUtil.fileExtensions());
        return Map.of("fileUrl", fileUrl, "originalName", originalName != null ? originalName : fileUrl);
    }

    @GetMapping("/api/chats")
    public List<Chat> getChats(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "50") int size) {
        if (after != null) {
            return chatRepository.findByIdGreaterThanOrderByIdAsc(after, PageRequest.of(0, size));
        }
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

    @GetMapping("/api/chats/search")
    public List<Chat> searchChats(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        return chatRepository.searchByKeyword(q.trim());
    }

    @GetMapping("/api/chats/calendar")
    public Map<String, Long> getChatCalendar() {
        List<Object[]> rows = chatRepository.findChatCountByDate();
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    @GetMapping("/api/chats/by-date")
    public List<Chat> getChatsByDate(@RequestParam String date) {
        return chatRepository.findByDate(date);
    }

    @GetMapping("/api/chat/ai-search")
    public Map<String, String> aiSearch(@RequestParam String q) {
        String result = aiChatSearchService.search(q);
        return Map.of("result", result);
    }

    @PostMapping("/api/admin/migrate-embeddings")
    public Map<String, Object> triggerMigration() {
        chatMigrationService.migrate();
        return Map.of("message", "마이그레이션 시작됨. /api/admin/migration-status 로 진행상황 확인");
    }

    @GetMapping("/api/admin/migration-status")
    public Map<String, Object> getMigrationStatus() {
        return chatMigrationService.getStatus();
    }

    @GetMapping("/api/chats/images")
    public List<Chat> getImageChats() {
        return chatRepository.findImageMessages();
    }

    @DeleteMapping("/api/chats/{id}")
    public void deleteChat(@PathVariable Long id,
                           @RequestAttribute("firebaseUid") String firebaseUid) {
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        if (!chat.getWriterUid().equals(firebaseUid)) {
            throw new IllegalStateException("본인 메시지만 삭제할 수 있습니다.");
        }
        // IMAGE: 또는 FILE: 메시지인 경우 실제 파일도 삭제
        String message = chat.getMessage();
        if (message != null && (message.startsWith("IMAGE:") || message.startsWith("FILE:"))) {
            String url = message.substring(message.indexOf(':') + 1);
            fileStorageService.delete(url);
        }
        chatRepository.deleteById(id);
        redisPublisher.publish("chat:delete", Map.of("id", id));
    }

    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        redisPublisher.publish("chat:typing", payload);
    }

    @MessageMapping("/chat")
    public void handleMessage(ChatMessage request, java.security.Principal principal) {
        if (principal != null) {
            request.setWriterUid(principal.getName());
        }

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm"));
        request.setSendTime(formattedTime);

        Chat saved = chatService.save(request);
        redisPublisher.publish("chat:message", saved);

        try {
            MemberCacheDto sender = memberService.findByProviderId(request.getWriterUid());
            if (sender != null && sender.getCoupleId() != null) {
                List<MemberCacheDto> coupleMembers = memberService.findAllByCoupleId(sender.getCoupleId());
                for (MemberCacheDto partner : coupleMembers) {
                    if (!partner.getId().equals(sender.getId()) && partner.getFcmToken() != null) {
                        String body = request.getMessage();
                        if (body != null && body.startsWith("IMAGE:")) body = "사진을 보냈습니다";
                        if (body != null && body.startsWith("FILE:")) body = "파일을 보냈습니다";
                        fcmService.sendPush(partner.getFcmToken(), sender.getNickname(), body);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("FCM 전송 실패: {}", e.getMessage());
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String writerUid;
    }
}
