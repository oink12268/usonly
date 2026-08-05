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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

        // DB 검증 먼저: 앨범이 존재하지 않으면 파일 저장 자체를 하지 않음 (고아 파일 방지)
        Album album = null;
        if (albumId != null) {
            album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ALBUM_NOT_FOUND));
            if (!album.getCouple().getId().equals(couple.getId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }

        String mediaUrl;
        String thumbnailUrl = null;

        try {
            if ("IMAGE".equalsIgnoreCase(type)) {
                FileStorageService.StoreResult result = fileStorageService.storeImageWithThumbnail(couple.getId(), file);
                mediaUrl = result.url();
                thumbnailUrl = result.thumbnailUrl();
            } else {
                mediaUrl = fileStorageService.store(couple.getId(), file, FileUploadUtil.imageAndVideoExtensions());
                if (thumbnail != null && !thumbnail.isEmpty()) {
                    thumbnailUrl = fileStorageService.storeImage(couple.getId(), thumbnail);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }

        // 파일 저장 후 DB 저장 실패 시 파일 롤백
        final String savedMediaUrl = mediaUrl;
        final String savedThumbnailUrl = thumbnailUrl;

        try {
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
            // 커버 이미지 갱신까지 여기서 flush해서, 커밋 시점에 실패해도(트랜잭션 밖) 아니라
            // 이 try 블록 안에서 실패가 드러나게 함 → 아래 catch에서 파일 롤백 가능
            mediaRepository.flush();
        } catch (Exception e) {
            // DB 저장 실패 시 이미 저장된 파일 삭제 (고아 파일 방지). delete() 자체가 실패해도
            // 원래 예외(e)를 덮어쓰지 않도록 각각 별도로 방어
            safeDelete(savedMediaUrl);
            safeDelete(savedThumbnailUrl);
            throw e;
        }
    }

    private void safeDelete(String url) {
        try {
            fileStorageService.delete(url);
        } catch (Exception ex) {
            log.warn("고아 파일 정리 실패: {}", url, ex);
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
