package com.evho.usonly.domain.chat.entity;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.global.crypto.EncryptedStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    // 격리 리팩터링 이전 데이터는 null일 수 있음 (백필로 채워둠).
    // DB 레벨 NOT NULL 제약 대신 ChatService.save()에서 항상 채우도록 강제함.
    // @JsonIgnore 필수: ChatMessageSender가 저장된 Chat 엔티티를 그대로 Redis pub/sub(JSON)로
    // 브로드캐스트하는데, couple은 LAZY라 세션 종료 후 직렬화 시도하면
    // LazyInitializationException으로 실시간 전송이 매번 죽음 (couple_id는 FK라 프록시만으로 충분히
    // 저장되므로 이 필드 자체를 직렬화 대상에서 뺌 — ChatResponse DTO도 애초에 이 필드를 안 씀).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    @JsonIgnore
    private Couple couple;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String message;    // 대화 내용 (암호화 저장)
    private String writerUid;  // 보낸 사람

    private String sendTime;

    private Long replyToId;        // 답장 대상 메시지 ID
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String replyToMessage; // 답장 대상 메시지 내용 미리보기 (암호화 저장)
    private String replyToUid;     // 답장 대상 작성자

    @CreationTimestamp // 자동으로 현재 시간 저장
    private LocalDateTime createdAt;
}