package com.evho.usonly.domain.archive.controller;

import com.evho.usonly.domain.archive.dto.AlbumDetailResponse;
import com.evho.usonly.domain.archive.dto.AlbumResponse;
import com.evho.usonly.domain.archive.dto.MediaPhotoResponse;
import com.evho.usonly.domain.archive.service.ArchiveService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<Long>> createArchive(@RequestParam("title") String title,
                                                           @CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(archiveService.createAlbum(title, me.getId())));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadMedia(
            @RequestParam(value = "albumId", required = false) Long albumId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "takenAt", required = false) String takenAtStr,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @CurrentMember Member me) {
        LocalDateTime takenAt = (takenAtStr != null && !takenAtStr.isEmpty())
                ? LocalDateTime.parse(takenAtStr) : null;
        archiveService.uploadMedia(albumId, me.getId(), file, type, takenAt, thumbnail);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/media")
    public ResponseEntity<ApiResponse<List<MediaPhotoResponse>>> getAllMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(archiveService.getAllMedia(me.getId(), page, size)));
    }

    @GetMapping("/albums")
    public ResponseEntity<ApiResponse<List<AlbumResponse>>> getAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(archiveService.getAlbums(me.getId(), page, size)));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<ApiResponse<AlbumDetailResponse>> getAlbum(@PathVariable Long albumId,
                                                                      @CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(archiveService.getAlbumDetail(albumId, me.getId())));
    }

    @PutMapping("/{albumId}")
    public ResponseEntity<ApiResponse<Void>> updateAlbum(@PathVariable Long albumId,
                                                         @RequestParam String title,
                                                         @CurrentMember Member me) {
        archiveService.updateAlbumTitle(albumId, title, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{albumId}/cover")
    public ResponseEntity<ApiResponse<Void>> updateAlbumCover(@PathVariable Long albumId,
                                                              @RequestParam Long mediaId,
                                                              @CurrentMember Member me) {
        archiveService.updateAlbumCover(albumId, mediaId, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<ApiResponse<Void>> deleteAlbum(@PathVariable Long albumId,
                                                         @CurrentMember Member me) {
        archiveService.deleteAlbum(albumId, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @RequestMapping(value = "/reorder", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Void>> reorderAlbums(@RequestBody List<Long> albumIds,
                                                           @CurrentMember Member me) {
        archiveService.reorderAlbums(albumIds, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/media/{mediaId}/taken-at")
    public ResponseEntity<ApiResponse<Void>> updateMediaTakenAt(@PathVariable Long mediaId,
                                                                 @RequestParam String takenAt,
                                                                 @CurrentMember Member me) {
        LocalDateTime takenAtDt = LocalDateTime.parse(takenAt);
        archiveService.updateMediaTakenAt(mediaId, takenAtDt, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{albumId}/media/taken-at")
    public ResponseEntity<ApiResponse<Void>> updateAlbumMediaTakenAt(@PathVariable Long albumId,
                                                                      @RequestParam String takenAt,
                                                                      @CurrentMember Member me) {
        LocalDateTime takenAtDt = LocalDateTime.parse(takenAt);
        archiveService.updateAlbumMediaTakenAt(albumId, takenAtDt, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Long mediaId,
                                                         @CurrentMember Member me) {
        archiveService.deleteMedia(mediaId, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
