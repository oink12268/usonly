package com.evho.usonly.global.fcm;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendPush(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .putData("type", "chat")
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("chat_channel_v2")
                                .setTag("chat_message")  // 같은 tag = 이전 알림 교체 (백그라운드에서도 1개만 표시)
                                .setPriority(AndroidNotification.Priority.HIGH)
                                .setDefaultSound(true)
                                .build())
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            System.out.println("FCM 전송 실패: " + e.getMessage());
        }
    }

    public void sendAnniversaryPush(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .putData("type", "anniversary")
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("anniversary_channel")
                                .setPriority(AndroidNotification.Priority.HIGH)
                                .setDefaultSound(true)
                                .build())
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            System.out.println("기념일 FCM 전송 실패: " + e.getMessage());
        }
    }
}
