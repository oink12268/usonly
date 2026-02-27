package com.evho.usonly.domain.archive.dto;

import com.evho.usonly.domain.archive.entity.Album;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AlbumDetailResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private List<MediaItemResponse> mediaList;

    @Getter
    @AllArgsConstructor
    public static class MediaItemResponse {
        private Long id;
        private String mediaUrl;
        private String thumbnailUrl;
        private String mediaType;
    }

    // Entity를 DTO로 변환하는 정적 메서드
    public static AlbumDetailResponse of(Album album) {
        return AlbumDetailResponse.builder()
                .id(album.getId())
                .title(album.getTitle())
                .coverImageUrl(album.getCoverImageUrl())
                .mediaList(album.getMediaList().stream()
                        .map(m -> new MediaItemResponse(m.getId(), m.getMediaUrl(), m.getThumbnailUrl(), m.getMediaType()))
                        .toList())
                .build();
    }
}