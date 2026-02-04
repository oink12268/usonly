package com.evho.usonly.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // 1. resources 폴더에서 키 파일 읽어오기
            InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();

            // 2. Firebase 옵션 설정
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 3. 초기화 (이미 되어있으면 건너뛰기 - 스프링 재시작 시 오류 방지)
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("🔥 Firebase 연결 성공! (evho application) 🔥");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Firebase 연결 실패... 키 파일 위치를 확인하세요!");
        }
    }
}