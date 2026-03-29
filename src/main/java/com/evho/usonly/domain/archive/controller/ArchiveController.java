package com.evho.usonly.domain.archive.controller;

import com.evho.usonly.domain.archive.dto.AlbumDetailResponse;
import com.evho.usonly.domain.archive.dto.AlbumResponse;
import com.evho.usonly.domain.archive.dto.MediaPhotoResponse;
import com.evho.usonly.domain.archive.service.ArchiveService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @PostMapping("/create")
    public ResponseEntity<Long> createArchive(@RequestParam("title") String title,
                                              @CurrentMember Member me) {
        Long albumId = archiveService.createAlbum(title, me.getId());
        return ResponseEntity.ok(albumId);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMedia(
            @RequestParam(value = "albumId", required = false) Long albumId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "takenAt", required = false) String takenAtStr,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @CurrentMember Member me) {
        try {
            LocalDateTime takenAt = (takenAtStr != null && !takenAtStr.isEmpty())
                    ? LocalDateTime.parse(takenAtStr) : null;
            archiveService.uploadMedia(albumId, me.getId(), file, type, takenAt, thumbnail);
            return ResponseEntity.ok("업로드 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("실패: " + e.getMessage());
        }
    }

    @GetMapping("/media")
    public ResponseEntity<List<MediaPhotoResponse>> getAllMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @CurrentMember Member me) {
        return ResponseEntity.ok(archiveService.getAllMedia(me.getId(), page, size));
    }

    @GetMapping("/albums")
    public ResponseEntity<List<AlbumResponse>> getAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @CurrentMember Member me) {
        return ResponseEntity.ok(archiveService.getAlbums(me.getId(), page, size));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumDetailResponse> getAlbum(@PathVariable Long albumId,
                                                         @CurrentMember Member me) {
        return ResponseEntity.ok(archiveService.getAlbumDetail(albumId, me.getId()));
    }

    @PutMapping("/{albumId}")
    public ResponseEntity<String> updateAlbum(@PathVariable Long albumId,
                                              @RequestParam String title,
                                              @CurrentMember Member me) {
        archiveService.updateAlbumTitle(albumId, title, me.getId());
        return ResponseEntity.ok("수정 완료");
    }

    @PutMapping("/{albumId}/cover")
    public ResponseEntity<String> updateAlbumCover(@PathVariable Long albumId,
                                                   @RequestParam Long mediaId,
                                                   @CurrentMember Member me) {
        archiveService.updateAlbumCover(albumId, mediaId, me.getId());
        return ResponseEntity.ok("커버 이미지 변경 완료");
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<String> deleteAlbum(@PathVariable Long albumId,
                                              @CurrentMember Member me) {
        archiveService.deleteAlbum(albumId, me.getId());
        return ResponseEntity.ok("삭제 완료");
    }

    @RequestMapping(value = "/reorder", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<String> reorderAlbums(@RequestBody List<Long> albumIds,
                                                @CurrentMember Member me) {
        archiveService.reorderAlbums(albumIds, me.getId());
        return ResponseEntity.ok("순서 변경 완료");
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long mediaId,
                                              @CurrentMember Member me) {
        archiveService.deleteMedia(mediaId, me.getId());
        return ResponseEntity.ok("삭제 완료");
    }
}
