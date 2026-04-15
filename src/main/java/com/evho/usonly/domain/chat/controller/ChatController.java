package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatCalendarEntry;
import com.evho.usonly.domain.chat.dto.ChatFileUploadResponse;
import com.evho.usonly.domain.chat.dto.ChatImageUploadResponse;
import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.dto.ChatResponse;
import com.evho.usonly.domain.chat.dto.MigrationStatusResponse;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.AiChatSearchService;
import com.evho.usonly.domain.chat.service.ChatMigrationService;
import com.evho.usonly.domain.chat.service.ChatService;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.common.ApiResponse;
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
    public ApiResponse<ChatImageUploadResponse> uploadChatImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = fileStorageService.storeImage(file);
        return ApiResponse.ok(new ChatImageUploadResponse(imageUrl));
    }

    @PostMapping("/api/chat/file")
    public ApiResponse<ChatFileUploadResponse> uploadChatFile(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileUrl = fileStorageService.store(file, FileUploadUtil.fileExtensions());
        return ApiResponse.ok(new ChatFileUploadResponse(fileUrl, originalName != null ? originalName : fileUrl));
    }

    @GetMapping("/api/chats")
    public ApiResponse<List<ChatResponse>> getChats(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "50") int size) {
        if (after != null) {
            return ApiResponse.ok(chatRepository.findByIdGreaterThanOrderByIdAsc(after, PageRequest.of(0, size))
                    .stream().map(ChatResponse::from).toList());
        }
        List<Chat> chats;
        if (before == null) {
            chats = chatRepository.findByOrderByIdDesc(PageRequest.of(0, size));
        } else {
            chats = chatRepository.findByIdLessThanOrderByIdDesc(before, PageRequest.of(0, size));
        }
        List<Chat> reversed = new ArrayList<>(chats);
        java.util.Collections.reverse(reversed);
        return ApiResponse.ok(reversed.stream().map(ChatResponse::from).toList());
    }

    @GetMapping("/api/chats/search")
    public ApiResponse<List<ChatResponse>> searchChats(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) return ApiResponse.ok(List.of());
        return ApiResponse.ok(chatRepository.searchByKeyword(q.trim())
                .stream().map(ChatResponse::from).toList());
    }

    @GetMapping("/api/chats/calendar")
    public ApiResponse<List<ChatCalendarEntry>> getChatCalendar() {
        List<Object[]> rows = chatRepository.findChatCountByDate();
        List<ChatCalendarEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ChatCalendarEntry(row[0].toString(), ((Number) row[1]).longValue()));
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/api/chats/by-date")
    public ApiResponse<List<ChatResponse>> getChatsByDate(@RequestParam String date) {
        return ApiResponse.ok(chatRepository.findByDate(date)
                .stream().map(ChatResponse::from).toList());
    }

    @GetMapping("/api/chat/ai-search")
    public ApiResponse<String> aiSearch(@RequestParam String q) {
        return ApiResponse.ok(aiChatSearchService.search(q));
    }

    @PostMapping("/api/admin/migrate-embeddings")
    public ApiResponse<Void> triggerMigration() {
        chatMigrationService.migrate();
        return ApiResponse.ok();
    }

    @GetMapping("/api/admin/migration-status")
    public ApiResponse<MigrationStatusResponse> getMigrationStatus() {
        return ApiResponse.ok(chatMigrationService.getStatus());
    }

    @GetMapping("/api/chats/images")
    public ApiResponse<List<ChatResponse>> getImageChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(chatRepository.findImageMessages(PageRequest.of(page, size))
                .stream().map(ChatResponse::from).toList());
    }

    @DeleteMapping("/api/chats/{id}")
    public ApiResponse<Void> deleteChat(@PathVariable Long id,
                                        @RequestAttribute("firebaseUid") String firebaseUid) {
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        if (!chat.getWriterUid().equals(firebaseUid)) {
            throw new IllegalStateException("본인 메시지만 삭제할 수 있습니다.");
        }
        String message = chat.getMessage();
        if (message != null && (message.startsWith("IMAGE:") || message.startsWith("FILE:"))) {
            String url = message.substring(message.indexOf(':') + 1);
            fileStorageService.delete(url);
        }
        chatRepository.deleteById(id);
        redisPublisher.publish("chat:delete", Map.of("id", id));
        return ApiResponse.ok();
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
