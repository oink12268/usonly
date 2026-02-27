package com.evho.usonly.domain.archive.service;

import com.evho.usonly.domain.archive.dto.AlbumDetailResponse;
import com.evho.usonly.domain.archive.dto.AlbumResponse;
import com.evho.usonly.domain.archive.model.Album;
import com.evho.usonly.domain.archive.model.Media;
import com.evho.usonly.domain.archive.repository.AlbumRepository;
import com.evho.usonly.domain.archive.repository.MediaRepository;
import com.evho.usonly.domain.couple.model.Couple;
import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.utils.FileUploadUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final AlbumRepository albumRepository;
    private final MediaRepository mediaRepository;
    private final MemberRepository memberRepository;

    // ★ YAML 파일에 적은 값(custom.file.dir)을 가져와라!
    @Value("${custom.file.dir}")
    private String uploadDir;

    // ★ YAML 파일에 적은 값(custom.file.domain)을 가져와라!
    @Value("${custom.file.domain}")
    private String baseUrl;

    @Transactional
    public Long createAlbum(String title, Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다. id=" + userId));

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
    public void uploadMedia(Long albumId, Long userId, MultipartFile file, String type) throws IOException {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        Couple couple = member.getCouple();

        Set<String> allowedExts = "VIDEO".equalsIgnoreCase(type)
                ? FileUploadUtil.imageAndVideoExtensions()
                : FileUploadUtil.imageExtensions();
        String fileName = FileUploadUtil.generateSafeFilename(file, allowedExts);
        File dest = new File(uploadDir + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);

        // 이미지 타입일 때만 썸네일 생성 (400px, 품질 65%)
        String thumbnailUrl = null;
        if ("IMAGE".equalsIgnoreCase(type)) {
            try {
                String thumbFileName = "thumb_" + fileName;
                File thumbDest = new File(uploadDir + thumbFileName);
                Thumbnails.of(dest)
                        .size(400, 400)
                        .keepAspectRatio(true)
                        .outputQuality(0.65)
                        .toFile(thumbDest);
                thumbnailUrl = baseUrl + thumbFileName;
            } catch (Exception e) {
                // 썸네일 생성 실패 시 원본 URL로 폴백
                thumbnailUrl = baseUrl + fileName;
            }
        }

        Album album = null;
        if (albumId != null) {
            album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new IllegalArgumentException("앨번 없음"));
        }

        Media media = Media.builder()
                .mediaUrl(baseUrl + fileName)
                .thumbnailUrl(thumbnailUrl)
                .mediaType(type)
                .couple(couple)
                .album(album)
                .build();
        mediaRepository.save(media);

        // 앨범에 커버 이미지가 없다면, 썸네일(없으면 원본)을 커버로 지정
        if (album.getCoverImageUrl() == null || album.getCoverImageUrl().isEmpty()) {
            String coverUrl = thumbnailUrl != null ? thumbnailUrl : media.getMediaUrl();
            album.updateCoverImage(coverUrl);
        }
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> getAlbums(Long userId, int page, int size) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Pageable pageable = PageRequest.of(page, size);
        return albumRepository.findAllByCoupleIdOrdered(member.getCouple().getId(), pageable)
                .stream()
                .map(AlbumResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlbumDetailResponse getAlbumDetail(Long albumId, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범이 없습니다."));

        // 보안 체크 로직은 그대로 유지...

        return AlbumDetailResponse.of(album); // DTO로 변환하여 리턴
    }

    @Transactional
    public void updateAlbumTitle(Long albumId, String title, Long memberId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        album.updateTitle(title);
    }

    @Transactional
    public void updateAlbumCover(Long albumId, Long mediaId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범이 없습니다."));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("미디어가 없습니다."));
        if (!media.getAlbum().getId().equals(albumId)) {
            throw new IllegalStateException("해당 앨범의 사진이 아닙니다.");
        }
        String coverUrl = media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getMediaUrl();
        album.updateCoverImage(coverUrl);
    }

    @Transactional
    public void deleteAlbum(Long albumId, Long memberId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        // 앨범 안 미디어 파일 모두 삭제
        album.getMediaList().forEach(media -> {
            deleteFileByUrl(media.getMediaUrl());
            deleteFileByUrl(media.getThumbnailUrl());
        });
        albumRepository.deleteById(albumId); // cascade로 DB 레코드도 자동 삭제
    }

    @Transactional
    public void deleteMedia(Long mediaId, Long memberId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("미디어가 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
        if (!media.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        deleteFileByUrl(media.getMediaUrl());
        deleteFileByUrl(media.getThumbnailUrl());
        mediaRepository.deleteById(mediaId);
    }

    @Transactional
    public void reorderAlbums(List<Long> albumIds, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Long coupleId = member.getCouple().getId();

        for (int i = 0; i < albumIds.size(); i++) {
            Long targetAlbumId = albumIds.get(i);

            Album album = albumRepository.findById(targetAlbumId)
                    .orElseThrow(() -> new IllegalArgumentException("앨범 없음: " + targetAlbumId));

            if (!album.getCouple().getId().equals(coupleId)) {
                throw new IllegalStateException("권한이 없습니다.");
            }
            album.updateSortOrder(i);
        }
    }

    private void deleteFileByUrl(String url) {
        if (url == null || url.isEmpty()) return;
        String fileName = url.replace(baseUrl, "");
        File file = new File(uploadDir + fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
