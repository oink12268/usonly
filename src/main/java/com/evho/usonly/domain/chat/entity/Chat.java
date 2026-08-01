package com.evho.usonly.domain.chat.entity;

import com.evho.usonly.domain.couple.entity.Couple;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // 격리 리팩터링 이전 데이터는 null일 수 있음 (ChatCoupleBackfillRunner가 채움).
    // DB 레벨 NOT NULL 제약 대신 ChatService.save()에서 항상 채우도록 강제함.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @Column(columnDefinition = "TEXT")
    private String message;    // 대화 내용
    private String writerUid;  // 보낸 사람

    private String sendTime;

    private Long replyToId;        // 답장 대상 메시지 ID
    private String replyToMessage; // 답장 대상 메시지 내용 (미리보기용)
    private String replyToUid;     // 답장 대상 작성자

    @CreationTimestamp // 자동으로 현재 시간 저장
    private LocalDateTime createdAt;
}