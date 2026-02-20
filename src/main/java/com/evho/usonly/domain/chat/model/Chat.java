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

    private Long replyToId;        // 답장 대상 메시지 ID
    private String replyToMessage; // 답장 대상 메시지 내용 (미리보기용)
    private String replyToUid;     // 답장 대상 작성자

    @CreationTimestamp // 자동으로 현재 시간 저장
    private LocalDateTime createdAt;
}