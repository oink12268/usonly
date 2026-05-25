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
import com.evho.usonly.domain.chat.service.ChatMessageSender;
import com.evho.usonly.domain.chat.service.ChatMigrationService;
import com.evho.usonly.domain.coupon.service.CouponService;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.common.ApiResponse;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import com.evho.usonly.global.fcm.FcmService;
import com.evho.usonly.global.fcm.PushType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatMessageSender chatMessageSender;
    private final AiChatSearchService aiChatSearchService;
    private final ChatMigrationService chatMigrationService;
    private final MemberService memberService;
    private final FcmService fcmService;
    private final RedisPublisher redisPublisher;
    private final FileStorageService fileStorageService;
    private final CouponService couponService;

    @PostMapping("/api/chat/image")
    public ApiResponse<ChatImageUploadResponse> uploadChatImage(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("firebaseUid") String firebaseUid) throws IOException {
        String imageUrl = fileStorageService.storeImage(file);

        // 비동기로 쿠폰 감지 (실패해도 이미지 업로드에 영향 없음)
        try {
            MemberCacheDto member = memberService.findByProviderId(firebaseUid);
            if (member != null && member.getCoupleId() != null) {
                couponService.detectAndSave(imageUrl, member.getCoupleId(), null);
            }
        } catch (Exception e) {
            log.warn("쿠폰 감지 트리거 실패: {}", e.getMessage());
        }

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
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));
        if (!chat.getWriterUid().equals(firebaseUid)) {
            throw new CustomException(ErrorCode.CHAT_DELETE_FORBIDDEN);
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

    @PostMapping("/api/chat/read")
    public ApiResponse<Void> markChatAsRead(@RequestAttribute("firebaseUid") String firebaseUid) {
        try {
            // 본인 기기 알림 소거 (FCM clear_chat)
            MemberCacheDto member = memberService.findByProviderId(firebaseUid);
            if (member != null && member.getFcmToken() != null) {
                fcmService.sendPush(member.getFcmToken(), PushType.CLEAR_CHAT, null, null);
            }
        } catch (Exception e) {
            log.warn("채팅 읽음 처리 실패: {}", e.getMessage());
        }
        return ApiResponse.ok();
    }

    // 알림 답장(Direct Reply)용 REST 엔드포인트
    // STOMP 연결 없이 간단한 텍스트 메시지를 전송할 때 사용
    @PostMapping("/api/chats")
    public ApiResponse<Void> sendChatViaRest(
            @RequestBody ChatRequest body,
            @RequestAttribute("firebaseUid") String firebaseUid) {
        ChatMessage request = new ChatMessage();
        request.setWriterUid(firebaseUid);
        request.setMessage(body.getMessage());
        chatMessageSender.send(request);
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
        chatMessageSender.send(request);
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String writerUid;
    }
}
