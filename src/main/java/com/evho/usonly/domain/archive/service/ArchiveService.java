package com.evho.usonly.domain.archive.service;

import com.evho.usonly.domain.archive.dto.ArchiveCreateRequest;
import com.evho.usonly.domain.archive.dto.ArchiveResponse;
import com.evho.usonly.domain.archive.dto.ArchiveWriteRequest;
import com.evho.usonly.domain.archive.model.Archive;
import com.evho.usonly.domain.archive.repository.ArchiveRepository;
import com.evho.usonly.domain.member.model.User;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final MemberRepository memberRepository; // 커플 ID 찾기용
    private final FileStorageService fileStorageService; // 사진 업로드용

    public Archive createArchive(ArchiveCreateRequest request, MultipartFile image) throws IOException, ExecutionException, InterruptedException {

        // 1. 작성자 정보 조회 (커플 ID를 알기 위해)
        User writer = memberRepository.findByUid(request.getWriterUid());
        if (writer == null || writer.getCoupleId() == null) {
            throw new IllegalArgumentException("커플 매칭이 안 된 유저는 글을 쓸 수 없습니다.");
        }

        // 2. 이미지 업로드 (파일이 있을 때만)
        String photoUrl = "";
        if (image != null && !image.isEmpty()) {
            // "archives" 라는 폴더 이름으로 저장
            photoUrl = fileStorageService.uploadFile(image, "archives");
        }

        // 3. Archive 객체 생성
        String archiveId = UUID.randomUUID().toString(); // 게시글 ID 생성

        Archive archive = Archive.builder()
                .id(archiveId)
                .coupleId(writer.getCoupleId()) // 작성자의 커플 ID 자동 주입
                .writerUid(writer.getUid())
                .title(request.getTitle())
                .content(request.getContent())
                .visitDate(request.getVisitDate())
                .photoUrl(photoUrl) // 구글이 준 URL 주입
                .createdAt(LocalDateTime.now().toString())
                .build();

        // 4. DB 저장
        return archiveRepository.save(archive);
    }

    public List<ArchiveResponse> getArchiveList(String uid) throws ExecutionException, InterruptedException {
        User me = memberRepository.findByUid(uid);
        if (me == null || me.getCoupleId() == null) {
            throw new IllegalArgumentException("커플이 아니면 조회를 할 수 없습니다.");
        }
        var archives = archiveRepository.findAllByCoupleId(me.getCoupleId());

        return archives.stream()
                .map(ArchiveResponse::from)
                .toList();
    }

    public void write(ArchiveWriteRequest request, String photoUrl) throws ExecutionException, InterruptedException {
        User writer = memberRepository.findByUid(request.getWriterUid());

        Archive archive = new Archive();
        archive.setId(UUID.randomUUID().toString());

        if (writer != null) {
            archive.setCoupleId(writer.getCoupleId());
        }

        archive.setTitle(request.getTitle());
        archive.setContent(request.getContent());
        archive.setWriterUid(request.getWriterUid());
        archive.setPhotoUrl(photoUrl); // 이미지 주소 저장
        archive.setVisitDate(request.getVisitDate());

        archiveRepository.save(archive);
    }
}