package com.evho.usonly.domain.archive.entity;

import com.evho.usonly.domain.couple.entity.Couple;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수 (보안상 protected 추천)
@AllArgsConstructor
@Builder
public class Media {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    @JsonIgnore
    private Album album;

    private String mediaUrl;
    private String mediaType;     // 항상 IMAGE. 영상 업로드 지원 전 만들어진 기존 row에만 VIDEO가 남아있을 수 있음
    private String thumbnailUrl;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;  // 사진 촬영일 (클라이언트가 보내는 메타데이터)

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void updateTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }
}
