package com.evho.usonly.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // 1순위: 외부 파일 (k8s Secret 마운트)
            // 2순위: classpath (로컬 개발용)
            InputStream serviceAccount;
            Path externalPath = Path.of("/etc/firebase/serviceAccountKey.json");
            if (Files.exists(externalPath)) {
                serviceAccount = new FileInputStream(externalPath.toFile());
            } else {
                serviceAccount = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
            }

            if (serviceAccount == null) {
                throw new FileNotFoundException("serviceAccountKey.json을 찾을 수 없습니다.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Application Initialized");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
        }
    }
}