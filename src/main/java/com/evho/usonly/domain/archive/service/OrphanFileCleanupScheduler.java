package com.evho.usonly.domain.archive.service;

import com.evho.usonly.domain.archive.entity.Album;
import com.evho.usonly.domain.archive.entity.Media;
import com.evho.usonly.domain.archive.repository.MediaRepository;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

    private final MediaRepository mediaRepository;
    private final ChatRepository chatRepository;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    // 매주 일요일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * 0", zone = "Asia/Seoul")
    @Transactional
    public void cleanOrphanFiles() {
        log.info("고아 파일 정리 스케줄러 시작");
        cleanOrphanDiskFiles();
        cleanOrphanDbRecords();
        log.info("고아 파일 정리 스케줄러 완료");
    }

    // Case 1: 파일은 있는데 DB에 없는 경우 → 파일 삭제
    private void cleanOrphanDiskFiles() {
        // Media 파일 URL (원본 + 썸네일)
        Set<String> registeredUrls = mediaRepository.findAll().stream()
                .flatMap(m -> Stream.of(m.getMediaUrl(), m.getThumbnailUrl()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 채팅 이미지/파일 URL도 등록 URL에 포함 (IMAGE:url, FILE:url 형태)
        chatRepository.findAll().stream()
                .map(c -> c.getMessage())
                .filter(Objects::nonNull)
                .filter(msg -> msg.startsWith("IMAGE:") || msg.startsWith("FILE:"))
                .map(msg -> msg.substring(msg.indexOf(':') + 1))
                .filter(url -> url.startsWith(baseUrl)) // 로컬 파일만
                .forEach(registeredUrls::add);

        File root = new File(uploadDir);
        if (!root.exists()) {
            log.warn("업로드 디렉토리가 존재하지 않음: {}", uploadDir);
            return;
        }

        int deleted = 0;
        int skipped = 0;

        try (Stream<Path> paths = Files.walk(root.toPath())) {
            for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String relativePath = root.toPath().relativize(path).toString().replace("\\", "/");
                String fileUrl = baseUrl + relativePath;

                // {coupleId}/notes/ 하위(메모 이미지)와 profiles/ 하위(프로필 사진)는 Media/Chat
                // registeredUrls에 안 잡히는 게 정상이라 별도 관리 대상으로 보고 제외
                if (relativePath.matches("\\d+/notes/.*") || relativePath.startsWith("notes/")
                        || relativePath.startsWith("profiles/")) {
                    skipped++;
                    continue;
                }

                if (!registeredUrls.contains(fileUrl)) {
                    // 삭제 전 로그만 남기고 실제 삭제는 하지 않음 (dry-run)
                    // 충분히 검증된 후 아래 주석 해제
                    log.warn("[Case1][DRY-RUN] 고아 파일 발견 (삭제 안 함): {}", fileUrl);
                    // boolean success = path.toFile().delete();
                    // if (success) { log.info("[Case1] 고아 파일 삭제: {}", fileUrl); deleted++; }
                    deleted++; // dry-run 카운트
                }
            }
        } catch (IOException e) {
            log.error("디스크 고아 파일 정리 중 오류 발생", e);
        }

        log.info("Case1 완료 - 고아 파일 발견(dry-run): {}개, 제외(notes): {}개", deleted, skipped);
    }

    // Case 2: DB엔 있는데 파일이 없는 경우 → Media 레코드 삭제 + 앨범 커버 갱신
    private void cleanOrphanDbRecords() {
        List<Media> allMedia = mediaRepository.findAll();
        int deleted = 0;

        for (Media media : allMedia) {
            // 로컬 파일이 아닌 외부 URL은 체크 제외
            if (!isLocalUrl(media.getMediaUrl())) continue;

            boolean mediaFileMissing = isLocalFileMissing(media.getMediaUrl());
            if (!mediaFileMissing) continue; // 원본 파일 있으면 정상

            log.info("[Case2] 파일 없는 Media 레코드 발견: id={}, url={}", media.getId(), media.getMediaUrl());

            // 앨범 커버였으면 커버 갱신
            Album album = media.getAlbum();
            if (album != null) {
                String coverCandidate = media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getMediaUrl();
                if (coverCandidate.equals(album.getCoverImageUrl())) {
                    List<Media> remaining = mediaRepository.findByAlbumIdOrderByCreatedAtAsc(album.getId())
                            .stream()
                            .filter(m -> !m.getId().equals(media.getId()))
                            .collect(Collectors.toList());
                    if (remaining.isEmpty()) {
                        album.updateCoverImage(null);
                    } else {
                        Media next = remaining.get(0);
                        album.updateCoverImage(next.getThumbnailUrl() != null ? next.getThumbnailUrl() : next.getMediaUrl());
                    }
                }
            }

            // 썸네일 파일이 남아있으면 삭제
            if (media.getThumbnailUrl() != null && isLocalUrl(media.getThumbnailUrl())
                    && !isLocalFileMissing(media.getThumbnailUrl())) {
                String thumbPath = media.getThumbnailUrl().replace(baseUrl, uploadDir);
                new File(thumbPath).delete();
            }

            mediaRepository.deleteById(media.getId());
            deleted++;
        }

        log.info("Case2 완료 - 삭제된 DB 레코드: {}개", deleted);
    }

    // baseUrl로 시작하는 로컬 파일 URL인지 확인
    private boolean isLocalUrl(String url) {
        return url != null && url.startsWith(baseUrl);
    }

    // 로컬 파일이 실제로 존재하지 않는지 확인 (isLocalUrl 확인 후 호출)
    private boolean isLocalFileMissing(String url) {
        String filePath = url.replace(baseUrl, uploadDir);
        return !new File(filePath).exists();
    }
}
