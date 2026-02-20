package com.evho.usonly.domain.archive.dto;

import com.evho.usonly.domain.archive.model.Album;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AlbumResponse {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private String coverImageUrl; // 필요하다면 추가

    // Entity를 DTO로 변환하는 정적 메서드
    public static AlbumResponse of(Album album) {
        return new AlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getCreatedAt(),
                album.getCoverImageUrl()
        );
    }
}