package com.evho.usonly.domain.couple.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "couple")
public class Couple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    private String invitationCode;

    private LocalDateTime createdAt;

    // 저장 용량 사용량(바이트). 기존 row에는 컬럼 추가 시 NULL이 들어가므로 nullable로 두고
    // getStorageUsedBytes()에서 0으로 취급한다.
    private Long storageUsedBytes;

    // 생성자나 빌더 패턴 사용
    @Builder
    public Couple(LocalDate startDate, String invitationCode) {
        this.startDate = startDate;
        this.invitationCode = invitationCode;
        this.createdAt = LocalDateTime.now();
    }

    public long getStorageUsedBytes() {
        return storageUsedBytes == null ? 0L : storageUsedBytes;
    }
}