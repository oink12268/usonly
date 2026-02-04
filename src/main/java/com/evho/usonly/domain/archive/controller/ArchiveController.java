package com.evho.usonly.domain.archive.controller;

import com.evho.usonly.domain.archive.dto.ArchiveWriteRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.UUID;
import com.evho.usonly.domain.archive.dto.ArchiveCreateRequest;
import com.evho.usonly.domain.archive.dto.ArchiveResponse;
import com.evho.usonly.domain.archive.model.Archive;
import com.evho.usonly.domain.archive.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    // POST /api/archives
    // Content-Type: multipart/form-data
    @PostMapping
    public Archive createArchive(
            @ModelAttribute ArchiveCreateRequest request, // 글자 데이터
            @RequestPart(value = "image", required = false) MultipartFile image // 파일 데이터
    ) {
        try {
            return archiveService.createArchive(request, image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping
    public List<ArchiveResponse> getList(@RequestParam String uid) {
        try {
            return archiveService.getArchiveList(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    @PostMapping("/write")
    public String write(
            @RequestPart(value = "data") ArchiveWriteRequest request, // 글자 데이터
            @RequestPart(value = "image", required = false) MultipartFile image // 파일 데이터
    ) {
        try {
            String photoUrl = "";

            // 1. 이미지가 왔으면 저장하기
            if (image != null && !image.isEmpty()) {
                // 파일명 겹치지 않게 UUID 붙이기
                String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();

                String savePath = "C:/usonly_images/" + fileName;
                image.transferTo(new File(savePath)); // 실제 파일 저장

                // DB에는 "파일명"만 저장 (나중에 불러올 때 주소 앞부분 붙여줌)
                photoUrl = "http://localhost:8080/images/" + fileName;
            }

            // 2. 서비스에게 DB 저장시키기 (Service에 이 메소드 추가해야 함!)
             archiveService.write(request, photoUrl);

            return "성공";
        } catch (Exception e) {
            e.printStackTrace();
            return "실패";
        }
    }
}