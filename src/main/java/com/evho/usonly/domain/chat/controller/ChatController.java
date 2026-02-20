package com.evho.usonly.domain.chat.controller;

import com.evho.usonly.domain.chat.dto.ChatDto;
import com.evho.usonly.domain.chat.model.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.chat.service.ChatService;
import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.fcm.FcmService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {
    private final ChatRepository chatRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberRepository memberRepository;
    private final FcmService fcmService;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    @PostMapping("/api/chat/image")
    public Map<String, String> uploadChatImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(uploadDir + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);

        String imageUrl = baseUrl + fileName;
        return Map.of("imageUrl", imageUrl);
    }

    @GetMapping("/api/chats")
    public List<Chat> getChats() {
        return chatRepository.findAllByOrderByCreatedAtAsc();
    }

    @DeleteMapping("/api/chats/{id}")
    public void deleteChat(@PathVariable Long id) {
        chatRepository.deleteById(id);
        messagingTemplate.convertAndSend("/sub/chat/delete", Map.of("id", id));
    }

    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/sub/chat/typing", payload);
    }

    @MessageMapping("/chat")
    public void handleMessage(ChatDto request) {

        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm"));
        request.setSendTime(formattedTime);

        Chat saved = chatService.save(request);

        System.out.println("메시지 받음: " + request.getMessage());

        // (2) 구독자들에게 저장된 Chat 엔티티 뿌리기 (id 포함)
        messagingTemplate.convertAndSend("/sub/chat", saved);

        // (3) 상대방에게 FCM 푸시 전송
        sendPushToPartner(request);
    }

    private void sendPushToPartner(ChatDto request) {
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
