package com.evho.usonly.global.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    // notification 필드 없이 data-only로 전송
    // → FCM SDK가 직접 알림을 표시하지 않아 Flutter의 flutter_local_notifications가
    //   "답장" 버튼이 포함된 알림을 표시할 수 있음
    public void sendPush(String targetToken, PushType type, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message.Builder builder = Message.builder()
                .setToken(targetToken)
                .putData("type", type.value())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build());

        if (title != null || body != null) {
            builder.putData("title", title != null ? title : "")
                   .putData("body", body != null ? body : "");
        }

        try {
            FirebaseMessaging.getInstance().send(builder.build());
        } catch (Exception e) {
            log.warn("FCM 전송 실패 [{}]: {}", type.value(), e.getMessage());
        }
    }

}
