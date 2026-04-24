package com.evho.usonly.global.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendPush(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        // notification 필드 없이 data-only로 전송
        // → FCM SDK가 직접 알림을 표시하지 않아 Flutter의 flutter_local_notifications가
        //   "답장" 버튼이 포함된 알림을 표시할 수 있음
        Message message = Message.builder()
                .setToken(targetToken)
                .putData("type", "chat")
                .putData("title", title != null ? title : "")
                .putData("body", body != null ? body : "")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM 전송 실패: {}", e.getMessage());
        }
    }

    // 모바일 알림 소거용 silent FCM (알림 없이 data만 전달)
    public void sendClearNotification(String targetToken) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .putData("type", "clear_chat")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM clear 전송 실패: {}", e.getMessage());
        }
    }

    public void sendAnniversaryPush(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .putData("type", "anniversary")
                .putData("title", title != null ? title : "")
                .putData("body", body != null ? body : "")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("기념일 FCM 전송 실패: {}", e.getMessage());
        }
    }
}
