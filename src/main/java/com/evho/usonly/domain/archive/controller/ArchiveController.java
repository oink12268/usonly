package com.evho.usonly.domain.archive.controller;


import com.evho.usonly.domain.archive.dto.AlbumDetailResponse;
import com.evho.usonly.domain.archive.dto.AlbumResponse;
import com.evho.usonly.domain.archive.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @PostMapping("/create")
    public ResponseEntity<Long> createArchive(@RequestParam("title") String title,
                                              @RequestParam("userId") Long userId) {
        // 서비스가 userId를 가지고 Member를 찾아서 저장해줄 겁니다.
        Long albumId = archiveService.createAlbum(title, userId);
        return ResponseEntity.ok(albumId);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMedia(
            @RequestParam(value = "albumId", required = false) Long albumId,
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        try {
            archiveService.uploadMedia(albumId, userId, file, type);
            return ResponseEntity.ok("업로드 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("실패: " + e.getMessage());
        }
    }

    @GetMapping("/albums")
    public ResponseEntity<List<AlbumResponse>> getAlbums(@RequestParam Long userId) {
        List<AlbumResponse> albums = archiveService.getAlbums(userId);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumDetailResponse> getAlbum(
            @PathVariable Long albumId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(archiveService.getAlbumDetail(albumId, userId));
    }

    @PutMapping("/{albumId}")
    public ResponseEntity<String> updateAlbum(@PathVariable Long albumId,
                                              @RequestParam String title) {
        archiveService.updateAlbumTitle(albumId, title);
        return ResponseEntity.ok("수정 완료");
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<String> deleteAlbum(@PathVariable Long albumId) {
        archiveService.deleteAlbum(albumId);
        return ResponseEntity.ok("삭제 완료");
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long mediaId) {
        archiveService.deleteMedia(mediaId);
        return ResponseEntity.ok("삭제 완료");
    }
}