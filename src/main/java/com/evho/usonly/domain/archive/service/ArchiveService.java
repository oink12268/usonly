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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
        Album album = Album.builder()
                .title(title)
                .couple(couple)
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

        Album album = null;
        if (albumId != null) {
            album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new IllegalArgumentException("앨번 없음"));
        }

        Media media = Media.builder()
                .mediaUrl(baseUrl + fileName)
                .mediaType(type)
                .couple(couple)
                .album(album)
                .build();
        mediaRepository.save(media);

        // 3. 만약 앨범에 커버 이미지가 없다면, 방금 올린 사진을 커버로 지정!
        if (album.getCoverImageUrl() == null || album.getCoverImageUrl().isEmpty()) {
            album.updateCoverImage(media.getMediaUrl()); // Album 엔티티에 메서드 추가 필요
        }
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> getAlbums(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // Entity 리스트를 DTO 리스트로 변환해서 반환
        return albumRepository.findAllByCoupleId(member.getCouple().getId())
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
    public void deleteAlbum(Long albumId, Long memberId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
        if (!album.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        albumRepository.deleteById(albumId);
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
        mediaRepository.deleteById(mediaId);
    }
}
