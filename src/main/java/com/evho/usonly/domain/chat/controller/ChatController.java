package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.AiChatSearchService;
import com.evho.usonly.domain.chat.service.ChatService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.fcm.FcmService;
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
    private final AiChatSearchService aiChatSearchService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberRepository memberRepository;
    private final FcmService fcmService;

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
        // 내림차순으로 가져왔으니 다시 오름차순으로 뒤집어서 반환
        List<Chat> result = new ArrayList<>(chats);
        java.util.Collections.reverse(result);
        return result;
    }

    @GetMapping("/api/chat/ai-search")
    public Map<String, String> aiSearch(@RequestParam String q) {
        String result = aiChatSearchService.search(q);
        return Map.of("result", result);
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
    public void handleMessage(ChatMessage request, java.security.Principal principal) {
        // 클라이언트가 보낸 writerUid를 서버 검증 uid로 덮어쓰기
        if (principal != null) {
            request.setWriterUid(principal.getName());
        }

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm"));
        request.setSendTime(formattedTime);

        Chat saved = chatService.save(request);

        System.out.println("메시지 받음: " + request.getMessage());

        // (2) 구독자들에게 저장된 Chat 엔티티 뿌리기 (id 포함)
        messagingTemplate.convertAndSend("/sub/chat", saved);

        // (3) 상대방에게 FCM 푸시 전송
        sendPushToPartner(request);
    }

    private void sendPushToPartner(ChatMessage request) {
        try {
            // 보낸 사람 찾기 (providerId = uid)
            Member sender = memberRepository.findByProviderId(request.getWriterUid())
                    .orElse(null);
            if (sender == null || sender.getCouple() == null) return;

            // 같은 커플의 멤버들 중 나를 제외한 상대방 찾기
            List<Member> coupleMembers = memberRepository.findAllByCoupleId(sender.getCouple().getId());
            for (Member partner : coupleMembers) {
                if (!partner.getId().equals(sender.getId()) && partner.getFcmToken() != null) {
                    String body = request.getMessage();
                    if (body.startsWith("IMAGE:")) {
                        body = "사진을 보냈습니다";
                    }
                    fcmService.sendPush(
                            partner.getFcmToken(),
                            sender.getNickname(),
                            body
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("푸시 전송 중 에러: " + e.getMessage());
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
        private String writerUid;
    }
}
