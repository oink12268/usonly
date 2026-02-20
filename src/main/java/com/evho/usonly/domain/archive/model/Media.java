package com.evho.usonly.domain.archive.model;

import com.evho.usonly.domain.couple.model.Couple;
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
    private String mediaType;     // IMAGE, VIDEO
    private String thumbnailUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
