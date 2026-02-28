package com.evho.usonly.global.kafka;

import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.fcm.FcmService;
import com.evho.usonly.global.kafka.event.ChatNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatNotificationConsumer {

    private final MemberRepository memberRepository;
    private final FcmService fcmService;

    @KafkaListener(topics = "chat-notification", groupId = "usonly-notification")
    public void consume(ChatNotificationEvent event) {
        try {
            Member sender = memberRepository.findByProviderId(event.getWriterUid()).orElse(null);
            if (sender == null || sender.getCouple() == null) return;

            List<Member> coupleMembers = memberRepository.findAllByCoupleId(sender.getCouple().getId());
            for (Member partner : coupleMembers) {
                if (!partner.getId().equals(sender.getId()) && partner.getFcmToken() != null) {
                    String body = event.getMessage();
                    if (body != null && body.startsWith("IMAGE:")) {
                        body = "사진을 보냈습니다";
                    }
                    fcmService.sendPush(partner.getFcmToken(), sender.getNickname(), body);
                }
            }
        } catch (Exception e) {
            System.out.println("Kafka consumer FCM 전송 실패: " + e.getMessage());
        }
    }
}
