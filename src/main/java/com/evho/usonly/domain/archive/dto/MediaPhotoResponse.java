package com.evho.usonly.domain.archive.dto;

import com.evho.usonly.domain.archive.entity.Media;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MediaPhotoResponse {

    private final Long id;
    private final String mediaUrl;
    private final String thumbnailUrl;
    private final String mediaType;
    private final LocalDateTime takenAt;
    private final LocalDateTime createdAt;

    private MediaPhotoResponse(Media media) {
        this.id = media.getId();
        this.mediaUrl = media.getMediaUrl();
        this.thumbnailUrl = media.getThumbnailUrl();
        this.mediaType = media.getMediaType();
        this.takenAt = media.getTakenAt();
        this.createdAt = media.getCreatedAt();
    }

    public static MediaPhotoResponse of(Media media) {
        return new MediaPhotoResponse(media);
    }
}
