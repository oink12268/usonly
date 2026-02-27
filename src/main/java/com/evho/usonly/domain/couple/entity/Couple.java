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

    // 생성자나 빌더 패턴 사용
    @Builder
    public Couple(LocalDate startDate, String invitationCode) {
        this.startDate = startDate;
        this.invitationCode = invitationCode;
        this.createdAt = LocalDateTime.now();
    }
}