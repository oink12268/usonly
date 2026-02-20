package com.evho.usonly.global.fcm;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendPush(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;

        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("chat_channel_v2")
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
}
