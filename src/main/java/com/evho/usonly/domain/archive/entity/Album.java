package com.evho.usonly.domain.archive.entity;

import com.evho.usonly.domain.couple.entity.Couple;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수 (보안상 protected 추천)
@AllArgsConstructor
@Builder
public class Album {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id") // DB 컬럼명과 맞춤
    private Couple couple;

    @Column(name = "cover_image_url") // DB 컬럼과 매핑
    private String coverImageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Archive 하나에 여러 Media(사진/영상)가 달림
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    private List<Media> mediaList = new ArrayList<>();

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}