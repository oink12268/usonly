package com.evho.usonly.domain.chat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;    // 대화 내용
    private String writerUid;  // 보낸 사람

    private String sendTime;
    @CreationTimestamp // 자동으로 현재 시간 저장
    private LocalDateTime createdAt;
}