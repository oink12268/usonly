package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.dto.ChatMessage;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.domain.notification.service.NotificationSettingService;
import com.evho.usonly.global.fcm.FcmService;
import com.evho.usonly.global.fcm.PushType;
import com.evho.usonly.global.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 채팅 메시지 저장 + Redis 발행 + FCM 알림을 한 곳에서 처리
 * WebSocket(handleMessage)과 REST(sendChatViaRest) 양쪽에서 공통으로 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageSender {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("a h:mm");

    private final ChatService chatService;
    private final RedisPublisher redisPublisher;
    private final MemberService memberService;
    private final FcmService fcmService;
    private final NotificationSettingService notificationSettingService;

    /**
     * 메시지 저장 → Redis 발행 → 상대방 FCM 알림
     */
    public Chat send(ChatMessage message) {
        if (message.getSendTime() == null) {
            message.setSendTime(LocalDateTime.now().format(TIME_FORMATTER));
        }

        Chat saved = chatService.save(message);
        // 커플별 채널로 발행 → 다른 커플에게 새어나가지 않도록. couple은 getReferenceById 프록시라
        // getId()는 DB 조회 없이 안전.
        redisPublisher.publish("chat:message:" + saved.getCouple().getId(), saved);
        pushToPartner(message.getWriterUid(), message.getMessage());
        return saved;
    }

    private void pushToPartner(String writerUid, String rawMessage) {
        try {
            MemberCacheDto sender = memberService.findByProviderId(writerUid);
            if (sender == null || sender.getCoupleId() == null) return;

            String body = resolveNotificationBody(rawMessage);
            List<MemberCacheDto> coupleMembers = memberService.findAllByCoupleId(sender.getCoupleId());
            for (MemberCacheDto partner : coupleMembers) {
                if (!partner.getId().equals(sender.getId())
                        && partner.getFcmToken() != null
                        && notificationSettingService.isChatEnabled(partner.getId())) {
                    fcmService.sendPush(partner.getFcmToken(), PushType.CHAT, sender.getNickname(), body);
                }
            }
        } catch (Exception e) {
            log.warn("FCM 전송 실패: {}", e.getMessage());
        }
    }

    // IMAGE:, FILE: 접두사를 알림용 텍스트로 변환
    private String resolveNotificationBody(String message) {
        if (message == null) return null;
        if (message.startsWith("IMAGE:")) return "사진을 보냈습니다";
        if (message.startsWith("FILE:")) return "파일을 보냈습니다";
        return message;
    }
}
