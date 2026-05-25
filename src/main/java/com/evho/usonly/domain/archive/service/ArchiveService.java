package com.evho.usonly.domain.archive.service;

import com.evho.usonly.domain.archive.dto.AlbumDetailResponse;
import com.evho.usonly.domain.archive.dto.AlbumResponse;
import com.evho.usonly.domain.archive.dto.MediaPhotoResponse;
import com.evho.usonly.domain.archive.entity.Album;
import com.evho.usonly.domain.archive.entity.Media;
import com.evho.usonly.domain.archive.repository.AlbumRepository;
import com.evho.usonly.domain.archive.repository.MediaRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.storage.FileStorageService;
import com.evho.usonly.global.utils.FileUploadUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final AlbumRepository albumRepository;
    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Long createAlbum(String title, Member member) {
        Couple couple = member.getCouple();
        Integer minSortOrder = albumRepository.findMinSortOrderByCoupleId(couple.getId());
        int newSortOrder = (minSortOrder != null) ? minSortOrder - 1 : 0;

        Album album = Album.builder()
                .title(title)
                .couple(couple)
                .sortOrder(newSortOrder)
                .build();

        return albumRepository.save(album).getId();
    }

    @Transactional
    public void uploadMedia(Long albumId, Member member, MultipartFile file, String type, LocalDateTime takenAt, MultipartFile thumbnail) {
        Couple couple = member.getCouple();

        String mediaUrl;
        String thumbnailUrl = null;

        try {
            if ("IMAGE".equalsIgnoreCase(type)) {
                FileStorageService.StoreResult result = fileStorageService.storeImageWithThumbnail(file);
                mediaUrl = result.url();
                thumbnailUrl = result.thumbnailUrl();
            } else {
                mediaUrl = fileStorageService.store(file, FileUploadUtil.imageAndVideoExtensions());
                if (thumbnail != null && !thumbnail.isEmpty()) {
                    thumbnailUrl = fileStorageService.storeImage(thumbnail);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }

        Album album = null;
        if (albumId != null) {
            album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        }

        Media media = Media.builder()
                .mediaUrl(mediaUrl)
                .thumbnailUrl(thumbnailUrl)
                .mediaType(type)
                .couple(couple)
                .album(album)
                .takenAt(takenAt)
                .build();
        mediaRepository.save(media);

        if (album != null && (album.getCoverImageUrl() == null || album.getCoverImageUrl().isEmpty())) {
            String coverUrl = thumbnailUrl != null ? thumbnailUrl : mediaUrl;
            album.updateCoverImage(coverUrl);
        }
    }

    @Transactional(readOnly = true)
    public List<MediaPhotoResponse> getAllMedia(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return mediaRepository.findAllByCoupleIdOrderByDate(member.getCouple().getId(), pageable)
                .stream()
                .map(MediaPhotoResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> getAlbums(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return albumRepository.findAllByCoupleIdOrdered(member.getCouple().getId(), pageable)
                .stream()
                .map(AlbumResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlbumDetailResponse getAlbumDetail(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        return AlbumDetailResponse.of(album);
    }

    @Transactional
    public void updateAlbumTitle(Long albumId, String title, Member member) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        album.updateTitle(title);
    }

    @Transactional
    public void updateAlbumCover(Long albumId, Long mediaId, Member member) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDIA_NOT_FOUND));
        if (!media.getAlbum().getId().equals(albumId)) {
            throw new CustomException(ErrorCode.MEDIA_NOT_IN_ALBUM);
        }
        String coverUrl = media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getMediaUrl();
        album.updateCoverImage(coverUrl);
    }

    @Transactional
    public void deleteAlbum(Long albumId, Member member) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        album.getMediaList().forEach(media -> {
            fileStorageService.delete(media.getMediaUrl());
            fileStorageService.delete(media.getThumbnailUrl());
        });
        albumRepository.deleteById(albumId);
    }

    @Transactional
    public void deleteMedia(Long mediaId, Member member) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDIA_NOT_FOUND));
        if (!media.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Album album = media.getAlbum();
        fileStorageService.delete(media.getMediaUrl());
        fileStorageService.delete(media.getThumbnailUrl());
        mediaRepository.deleteById(mediaId);

        // 삭제한 사진이 앨범 커버였으면 커버 갱신
        if (album != null) {
            String deletedUrl = media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getMediaUrl();
            if (deletedUrl.equals(album.getCoverImageUrl())) {
                List<Media> remaining = mediaRepository.findByAlbumIdOrderByCreatedAtAsc(album.getId());
                if (remaining.isEmpty()) {
                    album.updateCoverImage(null);
                } else {
                    Media next = remaining.get(0);
                    album.updateCoverImage(next.getThumbnailUrl() != null ? next.getThumbnailUrl() : next.getMediaUrl());
                }
            }
        }
    }

    @Transactional
    public void updateMediaTakenAt(Long mediaId, LocalDateTime takenAt, Member member) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDIA_NOT_FOUND));
        if (!media.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        media.updateTakenAt(takenAt);
    }

    @Transactional
    public void reorderAlbums(List<Long> albumIds, Member member) {
        Long coupleId = member.getCouple().getId();

        Map<Long, Album> albumMap = albumRepository.findAllById(albumIds).stream()
                .collect(Collectors.toMap(Album::getId, a -> a));

        for (int i = 0; i < albumIds.size(); i++) {
            Long targetAlbumId = albumIds.get(i);
            Album album = albumMap.get(targetAlbumId);
            if (album == null) throw new CustomException(ErrorCode.ALBUM_NOT_FOUND);
            if (!album.getCouple().getId().equals(coupleId)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            album.updateSortOrder(i);
        }
    }
}
